package com.meetpgandhi.edots.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.CarrierConfigEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OrderEventEntity;
import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.CarrierConfigRepository;
import com.meetpgandhi.edots.domain.repository.OrderEventRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.statemachine.OrderStateMachine;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import com.meetpgandhi.edots.webhook.dto.CarrierWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrierWebhookServiceTest {

    @Mock
    private CarrierConfigRepository carrierConfigRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private OrderStateMachine stateMachine;

    @Mock
    private OrderEventProducer eventProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CarrierWebhookService service;

    private CarrierConfigEntity fastshipConfig;
    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() throws Exception {
        service = new CarrierWebhookService(
            carrierConfigRepository, orderRepository, orderEventRepository,
            stateMachine, eventProducer, objectMapper
        );

        String testKey = "secret-key-123";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String keyHash = HexFormat.of().formatHex(md.digest(testKey.getBytes(StandardCharsets.UTF_8)));

        fastshipConfig = new CarrierConfigEntity();
        fastshipConfig.setCarrierId("fastship");
        fastshipConfig.setCarrierName("FastShip Logistics");
        fastshipConfig.setApiKeyHash(keyHash);
        fastshipConfig.setStageMapping("{\"IN_TRANSIT\":\"DISPATCHED\",\"OUT_DELIVERY\":\"OUT_FOR_DELIVERY\"}");
        fastshipConfig.setActive(true);

        sampleOrder = new OrderEntity();
        sampleOrder.setId(50L);
        sampleOrder.setOrderReference("ORD-5050");
        sampleOrder.setCurrentStage(Stage.PACKED);
    }

    @Test
    @DisplayName("Valid webhook authenticates, maps status, and transitions order")
    void shouldProcessValidWebhookSuccessfully() {
        when(carrierConfigRepository.findByCarrierIdAndActiveTrue("fastship"))
            .thenReturn(Optional.of(fastshipConfig));
        when(orderRepository.findByOrderReference("ORD-5050"))
            .thenReturn(Optional.of(sampleOrder));

        OrderEvent expectedEvent = new OrderEvent(
            "evt-99", 50L, "ORD-5050", "DISPATCHED",
            Stage.PACKED, Stage.DISPATCHED, Actor.carrier("fastship"), null, Map.of(), Instant.now()
        );
        when(stateMachine.createTransitionEvent(eq(50L), eq("ORD-5050"), eq(Stage.PACKED), eq(Stage.DISPATCHED), any(), any(), any()))
            .thenReturn(expectedEvent);

        CarrierWebhookPayload payload = new CarrierWebhookPayload("ORD-5050", "IN_TRANSIT", null, Map.of());
        OrderEvent result = service.processWebhook("fastship", "secret-key-123", payload);

        assertThat(result).isNotNull();
        assertThat(sampleOrder.getCurrentStage()).isEqualTo(Stage.DISPATCHED);
        assertThat(sampleOrder.getCarrierId()).isEqualTo("fastship");
        verify(orderRepository).save(sampleOrder);
        verify(orderEventRepository).save(any(OrderEventEntity.class));
        verify(eventProducer).publishOrderEvent(eq(expectedEvent), any(), any(), eq("FastShip Logistics"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Invalid API key returns 401 Unauthorized")
    void shouldRejectInvalidApiKey() {
        when(carrierConfigRepository.findByCarrierIdAndActiveTrue("fastship"))
            .thenReturn(Optional.of(fastshipConfig));

        CarrierWebhookPayload payload = new CarrierWebhookPayload("ORD-5050", "IN_TRANSIT", null, Map.of());

        assertThatThrownBy(() -> service.processWebhook("fastship", "wrong-key", payload))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401");
    }

    @Test
    @DisplayName("Unmapped status returns 400 Bad Request")
    void shouldRejectUnmappedStatus() {
        when(carrierConfigRepository.findByCarrierIdAndActiveTrue("fastship"))
            .thenReturn(Optional.of(fastshipConfig));
        when(orderRepository.findByOrderReference("ORD-5050"))
            .thenReturn(Optional.of(sampleOrder));

        CarrierWebhookPayload payload = new CarrierWebhookPayload("ORD-5050", "UNKNOWN_STATUS", null, Map.of());

        assertThatThrownBy(() -> service.processWebhook("fastship", "secret-key-123", payload))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }
}
