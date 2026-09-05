package com.meetpgandhi.edots.eventengine.producer;

import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventProducer producer;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    @BeforeEach
    void setUp() {
        producer = new OrderEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "orderEventsTopic", "edots.order.events");
        ReflectionTestUtils.setField(producer, "notificationTriggerTopic", "edots.notification.trigger");
        ReflectionTestUtils.setField(producer, "auditLogTopic", "edots.audit.log");
        ReflectionTestUtils.setField(producer, "rescheduleTriggerTopic", "edots.reschedule.trigger");
    }

    @Test
    @DisplayName("Publishing standard stage change sends order event, notification trigger, and audit log")
    void shouldPublishOrderAndNotificationEvents() {
        Actor actor = Actor.agent("agent1@edots.dev");
        OrderEvent event = new OrderEvent(
            "evt-1", 1L, "ORD-2026-0001", "STAGE_CHANGE",
            Stage.DISPATCHED, Stage.OUT_FOR_DELIVERY, actor, null, Map.of(), Instant.now()
        );

        producer.publishOrderEvent(event, "customer@example.com", "+919999999999", "Agent Ramesh", 0, 3);

        verify(kafkaTemplate).send(eq("edots.order.events"), eq("ORD-2026-0001"), eq(event));
        verify(kafkaTemplate).send(eq("edots.notification.trigger"), eq("ORD-2026-0001"), any());
        verify(kafkaTemplate).send(eq("edots.audit.log"), eq("ORD-2026-0001"), any());
        verify(kafkaTemplate, never()).send(eq("edots.reschedule.trigger"), any(), any());
    }

    @Test
    @DisplayName("Publishing FAILED_ATTEMPT also triggers reschedule topic")
    void shouldPublishRescheduleOnFailedAttempt() {
        Actor actor = Actor.agent("agent1@edots.dev");
        OrderEvent event = new OrderEvent(
            "evt-2", 2L, "ORD-2026-0002", "STAGE_CHANGE",
            Stage.OUT_FOR_DELIVERY, Stage.FAILED_ATTEMPT, actor, "RECIPIENT_ABSENT", Map.of(), Instant.now()
        );

        producer.publishOrderEvent(event, "customer@example.com", "+919999999999", "Agent Ramesh", 1, 3);

        verify(kafkaTemplate).send(eq("edots.order.events"), eq("ORD-2026-0002"), eq(event));
        verify(kafkaTemplate).send(eq("edots.notification.trigger"), eq("ORD-2026-0002"), any());
        verify(kafkaTemplate).send(eq("edots.audit.log"), eq("ORD-2026-0002"), any());
        verify(kafkaTemplate).send(eq("edots.reschedule.trigger"), eq("ORD-2026-0002"), any());
    }

    @Test
    @DisplayName("Publishing audit event sends directly to audit topic")
    void shouldPublishAuditEvent() {
        AuditLogEvent auditEvent = AuditLogEvent.of(
            "RULE_UPDATE", "admin@edots.dev", com.meetpgandhi.edots.domain.model.ActorType.ADMIN,
            "RULE-1", "NOTIFICATION_RULE", "Updated email template"
        );

        producer.publishAuditEvent(auditEvent);

        verify(kafkaTemplate).send(eq("edots.audit.log"), eq("RULE-1"), eq(auditEvent));
    }
}
