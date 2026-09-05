package com.meetpgandhi.edots.eventengine.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.AdminConfigEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OrderEventEntity;
import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.AdminConfigRepository;
import com.meetpgandhi.edots.domain.repository.OrderEventRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.statemachine.OrderStateMachine;
import com.meetpgandhi.edots.eventengine.model.RescheduleTriggerEvent;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoRescheduleConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private AdminConfigRepository adminConfigRepository;

    @Mock
    private OrderStateMachine stateMachine;

    @Mock
    private OrderEventProducer eventProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AutoRescheduleConsumer consumer;

    private OrderEntity order;

    @BeforeEach
    void setUp() {
        consumer = new AutoRescheduleConsumer(
            orderRepository, orderEventRepository, adminConfigRepository,
            stateMachine, eventProducer, objectMapper
        );

        order = new OrderEntity();
        order.setId(101L);
        order.setOrderReference("ORD-RESCHEDULE-1");
        order.setCurrentStage(Stage.FAILED_ATTEMPT);
        order.setMaxAttempts(3);
    }

    @Test
    @DisplayName("When attempt < max, auto-reschedule transitions back to OUT_FOR_DELIVERY and sets next slot")
    void shouldRescheduleWhenAttemptsLessThanMax() {
        order.setAttemptCount(1);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        AdminConfigEntity config = new AdminConfigEntity();
        config.setConfigKey("max_delivery_attempts");
        config.setConfigValue("3");
        when(adminConfigRepository.findByConfigKey("max_delivery_attempts")).thenReturn(Optional.of(config));

        OrderEvent expectedEvent = new OrderEvent(
            "evt-101", 101L, "ORD-RESCHEDULE-1", "OUT_FOR_DELIVERY",
            Stage.FAILED_ATTEMPT, Stage.OUT_FOR_DELIVERY, Actor.system(), "AUTO_RESCHEDULED", Map.of(), Instant.now()
        );
        when(stateMachine.createTransitionEvent(eq(101L), eq("ORD-RESCHEDULE-1"), eq(Stage.FAILED_ATTEMPT), eq(Stage.OUT_FOR_DELIVERY), any(), any(), any()))
            .thenReturn(expectedEvent);

        RescheduleTriggerEvent trigger = new RescheduleTriggerEvent(
            101L, "ORD-RESCHEDULE-1", 1, 3, "RECIPIENT_ABSENT", Instant.now()
        );

        consumer.processAutoReschedule(trigger);

        assertThat(order.getCurrentStage()).isEqualTo(Stage.OUT_FOR_DELIVERY);
        assertThat(order.getNextDeliverySlot()).isNotNull();
        assertThat(order.getNextDeliverySlot()).isAfter(LocalDateTime.now());
        verify(orderRepository).save(order);
        verify(orderEventRepository).save(any(OrderEventEntity.class));
        verify(eventProducer).publishOrderEvent(eq(expectedEvent), any(), any(), any(), eq(1), eq(3));
    }

    @Test
    @DisplayName("When attempt >= max, auto-reschedule transitions order to RETURNED")
    void shouldTransitionToReturnedWhenMaxAttemptsReached() {
        order.setAttemptCount(3);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        AdminConfigEntity config = new AdminConfigEntity();
        config.setConfigKey("max_delivery_attempts");
        config.setConfigValue("3");
        when(adminConfigRepository.findByConfigKey("max_delivery_attempts")).thenReturn(Optional.of(config));

        OrderEvent returnEvent = new OrderEvent(
            "evt-ret", 101L, "ORD-RESCHEDULE-1", "RETURNED",
            Stage.FAILED_ATTEMPT, Stage.RETURNED, Actor.system(), "MAX_ATTEMPTS_EXCEEDED", Map.of(), Instant.now()
        );
        when(stateMachine.createTransitionEvent(eq(101L), eq("ORD-RESCHEDULE-1"), eq(Stage.FAILED_ATTEMPT), eq(Stage.RETURNED), any(), any(), any()))
            .thenReturn(returnEvent);

        RescheduleTriggerEvent trigger = new RescheduleTriggerEvent(
            101L, "ORD-RESCHEDULE-1", 3, 3, "WRONG_ADDRESS", Instant.now()
        );

        consumer.processAutoReschedule(trigger);

        assertThat(order.getCurrentStage()).isEqualTo(Stage.RETURNED);
        verify(orderRepository).save(order);
        verify(orderEventRepository).save(any(OrderEventEntity.class));
        verify(eventProducer).publishOrderEvent(eq(returnEvent), any(), any(), any(), eq(3), eq(3));
        verify(eventProducer).publishAuditEvent(any());
    }
}
