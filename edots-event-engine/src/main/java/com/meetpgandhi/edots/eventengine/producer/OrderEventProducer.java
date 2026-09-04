package com.meetpgandhi.edots.eventengine.producer;

import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import com.meetpgandhi.edots.eventengine.model.NotificationTriggerEvent;
import com.meetpgandhi.edots.eventengine.model.RescheduleTriggerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Profile("!local")
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${edots.kafka.topics.order-events:edots.order.events}")
    private String orderEventsTopic;

    @Value("${edots.kafka.topics.notification-trigger:edots.notification.trigger}")
    private String notificationTriggerTopic;

    @Value("${edots.kafka.topics.audit-log:edots.audit.log}")
    private String auditLogTopic;

    @Value("${edots.kafka.topics.reschedule-trigger:edots.reschedule.trigger}")
    private String rescheduleTriggerTopic;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderEvent(
        OrderEvent event,
        String customerEmail,
        String customerPhone,
        String agentName,
        int currentAttemptCount,
        int maxAttempts
    ) {
        try {
            // 1. Publish to edots.order.events
            kafkaTemplate.send(orderEventsTopic, event.orderReference(), event);
            log.info("Published OrderEvent to {}: orderRef={}, stage={}",
                orderEventsTopic, event.orderReference(), event.newStage());

            // 2. Publish to edots.notification.trigger
            NotificationTriggerEvent notifyEvent = new NotificationTriggerEvent(
                event.orderId(),
                event.orderReference(),
                event.previousStage(),
                event.newStage(),
                customerEmail,
                customerPhone,
                agentName,
                event.reasonCode(),
                Instant.now()
            );
            kafkaTemplate.send(notificationTriggerTopic, event.orderReference(), notifyEvent);

            // 3. Publish to edots.audit.log
            AuditLogEvent auditEvent = new AuditLogEvent(
                "STAGE_TRANSITION_" + event.newStage().name(),
                event.actor().actorId(),
                event.actor().actorType(),
                event.orderReference(),
                "ORDER",
                String.format("Transitioned from %s to %s. Reason: %s",
                    event.previousStage(), event.newStage(), event.reasonCode()),
                Instant.now()
            );
            kafkaTemplate.send(auditLogTopic, event.orderReference(), auditEvent);

            // 4. If FAILED_ATTEMPT, publish to edots.reschedule.trigger
            if (event.newStage() == Stage.FAILED_ATTEMPT) {
                RescheduleTriggerEvent rescheduleEvent = new RescheduleTriggerEvent(
                    event.orderId(),
                    event.orderReference(),
                    currentAttemptCount,
                    maxAttempts,
                    event.reasonCode(),
                    Instant.now()
                );
                kafkaTemplate.send(rescheduleTriggerTopic, event.orderReference(), rescheduleEvent);
                log.info("Published RescheduleTriggerEvent to {}: orderRef={}, attempt={}/{}",
                    rescheduleTriggerTopic, event.orderReference(), currentAttemptCount, maxAttempts);
            }

        } catch (Exception ex) {
            log.error("Failed to publish Kafka event for orderRef={}: {}", event.orderReference(), ex.getMessage(), ex);
        }
    }

    public void publishAuditEvent(AuditLogEvent auditEvent) {
        try {
            kafkaTemplate.send(auditLogTopic, auditEvent.referenceId(), auditEvent);
            log.info("Published AuditLogEvent to {}: refId={}, eventType={}",
                auditLogTopic, auditEvent.referenceId(), auditEvent.eventType());
        } catch (Exception ex) {
            log.error("Failed to publish AuditLogEvent: {}", ex.getMessage(), ex);
        }
    }
}
