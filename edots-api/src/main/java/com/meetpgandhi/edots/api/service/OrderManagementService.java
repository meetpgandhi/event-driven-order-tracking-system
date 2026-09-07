package com.meetpgandhi.edots.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.api.dto.AgentDtos.StageTransitionRequest;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OrderEventEntity;
import com.meetpgandhi.edots.domain.exception.ResourceNotFoundException;
import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.OrderEventRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.statemachine.OrderStateMachine;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OrderManagementService {

    private static final Logger log = LoggerFactory.getLogger(OrderManagementService.class);

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderStateMachine stateMachine;
    private final OrderEventProducer eventProducer;
    private final OtpService otpService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public OrderManagementService(
        OrderRepository orderRepository,
        OrderEventRepository orderEventRepository,
        OrderStateMachine stateMachine,
        OrderEventProducer eventProducer,
        OtpService otpService,
        SimpMessagingTemplate messagingTemplate,
        ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.stateMachine = stateMachine;
        this.eventProducer = eventProducer;
        this.otpService = otpService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderEvent transitionOrder(
        Long orderId,
        Stage targetStage,
        Actor actor,
        String reasonCode,
        Map<String, Object> metadata
    ) {
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> ResourceNotFoundException.order(orderId));

        Stage currentStage = order.getCurrentStage();
        boolean isOtpVerified = (targetStage == Stage.DELIVERED) && otpService.isOtpVerified(orderId);

        // 1. Validate with State Machine
        stateMachine.validateTransition(currentStage, targetStage, reasonCode, isOtpVerified);

        // 2. Update Order Entity
        order.setCurrentStage(targetStage);
        order.setUpdatedAt(LocalDateTime.now());
        if (targetStage == Stage.FAILED_ATTEMPT) {
            order.setAttemptCount(order.getAttemptCount() + 1);
        }
        orderRepository.save(order);

        // 3. Persist Immutable OrderEventEntity
        OrderEventEntity eventEntity = new OrderEventEntity();
        eventEntity.setOrder(order);
        eventEntity.setEventType(targetStage.name());
        eventEntity.setPreviousStage(currentStage);
        eventEntity.setNewStage(targetStage);
        eventEntity.setActorId(actor.actorId());
        eventEntity.setActorType(actor.actorType());
        eventEntity.setReasonCode(reasonCode);
        if (metadata != null && !metadata.isEmpty()) {
            try {
                eventEntity.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                eventEntity.setMetadata(metadata.toString());
            }
        }
        eventEntity.setCreatedAt(LocalDateTime.now());
        orderEventRepository.save(eventEntity);

        // 4. Create and publish domain event
        OrderEvent domainEvent = stateMachine.createTransitionEvent(
            order.getId(),
            order.getOrderReference(),
            currentStage,
            targetStage,
            actor,
            reasonCode,
            metadata
        );

        String agentName = order.getAssignedAgent() != null ? order.getAssignedAgent().getName() : "Unassigned";
        eventProducer.publishOrderEvent(
            domainEvent,
            order.getCustomerEmail(),
            order.getCustomerPhone(),
            agentName,
            order.getAttemptCount(),
            order.getMaxAttempts()
        );

        // 5. Broadcast live update to WebSockets for Admin & live UI
        try {
            messagingTemplate.convertAndSend("/topic/dashboard-events", domainEvent);
        } catch (Exception e) {
            log.warn("Could not broadcast WebSocket message for order {}: {}", order.getOrderReference(), e.getMessage());
        }

        log.info("Successfully transitioned order {} from {} to {} by {}",
            order.getOrderReference(), currentStage, targetStage, actor);

        return domainEvent;
    }

    @Transactional
    public OrderEvent handleAgentTransition(Long orderId, String agentEmail, StageTransitionRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> ResourceNotFoundException.order(orderId));

        // Check that agent is assigned to this order if agent assignment exists
        if (order.getAssignedAgent() != null && !order.getAssignedAgent().getEmail().equalsIgnoreCase(agentEmail)) {
            throw new org.springframework.security.access.AccessDeniedException("You are not assigned to this order.");
        }

        Stage targetStage = Stage.valueOf(request.newStage().trim().toUpperCase());
        Actor actor = Actor.agent(agentEmail);

        return transitionOrder(orderId, targetStage, actor, request.reasonCode(), request.metadata());
    }
}
