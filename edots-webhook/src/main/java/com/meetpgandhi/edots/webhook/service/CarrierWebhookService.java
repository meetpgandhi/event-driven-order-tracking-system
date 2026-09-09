package com.meetpgandhi.edots.webhook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.CarrierConfigEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OrderEventEntity;
import com.meetpgandhi.edots.domain.exception.ResourceNotFoundException;
import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.CarrierConfigRepository;
import com.meetpgandhi.edots.domain.repository.OrderEventRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.statemachine.OrderStateMachine;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import com.meetpgandhi.edots.webhook.dto.CarrierWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Service
public class CarrierWebhookService {

    private static final Logger log = LoggerFactory.getLogger(CarrierWebhookService.class);

    private final CarrierConfigRepository carrierConfigRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderStateMachine stateMachine;
    private final OrderEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public CarrierWebhookService(
        CarrierConfigRepository carrierConfigRepository,
        OrderRepository orderRepository,
        OrderEventRepository orderEventRepository,
        OrderStateMachine stateMachine,
        OrderEventProducer eventProducer,
        ObjectMapper objectMapper
    ) {
        this.carrierConfigRepository = carrierConfigRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.stateMachine = stateMachine;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderEvent processWebhook(String carrierId, String apiKey, CarrierWebhookPayload payload) {
        // 1. Authenticate Carrier via API Key Hash
        CarrierConfigEntity carrier = carrierConfigRepository.findByCarrierIdAndActiveTrue(carrierId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown or inactive carrier: " + carrierId));

        if (!validateApiKey(apiKey, carrier.getApiKeyHash())) {
            eventProducer.publishAuditEvent(AuditLogEvent.of(
                "CARRIER_WEBHOOK_AUTH_FAILED", carrierId, com.meetpgandhi.edots.domain.model.ActorType.CARRIER,
                carrierId, "CARRIER", "Invalid API key supplied in X-Api-Key"
            ));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid carrier API key");
        }

        // 2. Validate Order Existence
        OrderEntity order = orderRepository.findByOrderReference(payload.orderReference().trim().toUpperCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order not found: " + payload.orderReference()));

        // 3. Map Carrier Status to EDOTS Stage
        Stage targetStage = mapCarrierStatus(carrier, payload.status());
        if (targetStage == null) {
            eventProducer.publishAuditEvent(AuditLogEvent.of(
                "CARRIER_WEBHOOK_UNMAPPED_STATUS", carrierId, com.meetpgandhi.edots.domain.model.ActorType.CARRIER,
                payload.orderReference(), "ORDER", "Unrecognized status: " + payload.status()
            ));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognized carrier status: " + payload.status());
        }

        // 4. Validate & Execute Transition
        Stage previousStage = order.getCurrentStage();
        stateMachine.validateTransition(previousStage, targetStage, payload.reasonCode(), true);

        order.setCurrentStage(targetStage);
        order.setCarrierId(carrierId);
        order.setUpdatedAt(LocalDateTime.now());
        if (targetStage == Stage.FAILED_ATTEMPT) {
            order.setAttemptCount(order.getAttemptCount() + 1);
        }
        orderRepository.save(order);

        // 5. Persist Immutable Event
        OrderEventEntity eventEntity = new OrderEventEntity();
        eventEntity.setOrder(order);
        eventEntity.setEventType(targetStage.name());
        eventEntity.setPreviousStage(previousStage);
        eventEntity.setNewStage(targetStage);
        eventEntity.setActorId(carrierId);
        eventEntity.setActorType(com.meetpgandhi.edots.domain.model.ActorType.CARRIER);
        eventEntity.setReasonCode(payload.reasonCode());
        eventEntity.setCreatedAt(LocalDateTime.now());
        orderEventRepository.save(eventEntity);

        // 6. Publish to Kafka
        Actor actor = Actor.carrier(carrierId);
        OrderEvent domainEvent = stateMachine.createTransitionEvent(
            order.getId(),
            order.getOrderReference(),
            previousStage,
            targetStage,
            actor,
            payload.reasonCode(),
            payload.metadata()
        );

        eventProducer.publishOrderEvent(
            domainEvent,
            order.getCustomerEmail(),
            order.getCustomerPhone(),
            carrier.getCarrierName(),
            order.getAttemptCount(),
            order.getMaxAttempts()
        );

        log.info("Processed carrier webhook from {} for order {}: mapped {} -> {}",
            carrierId, order.getOrderReference(), payload.status(), targetStage);

        return domainEvent;
    }

    private Stage mapCarrierStatus(CarrierConfigEntity carrier, String carrierStatus) {
        try {
            Map<String, String> mapping = objectMapper.readValue(
                carrier.getStageMapping(), new TypeReference<Map<String, String>>() {}
            );
            String edotsStageStr = mapping.get(carrierStatus.trim().toUpperCase());
            if (edotsStageStr != null) {
                return Stage.valueOf(edotsStageStr);
            }
        } catch (Exception ex) {
            log.error("Error parsing stage mapping for carrier {}: {}", carrier.getCarrierId(), ex.getMessage());
        }
        return null;
    }

    private boolean validateApiKey(String providedKey, String expectedHash) {
        if (providedKey == null || expectedHash == null) return false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] providedHashBytes = digest.digest(providedKey.getBytes(StandardCharsets.UTF_8));
            String providedHash = HexFormat.of().formatHex(providedHashBytes);
            return MessageDigest.isEqual(providedHash.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
