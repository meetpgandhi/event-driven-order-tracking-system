package com.meetpgandhi.edots.eventengine.local;

import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.eventengine.consumer.AuditLogConsumer;
import com.meetpgandhi.edots.eventengine.consumer.AutoRescheduleConsumer;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import com.meetpgandhi.edots.eventengine.model.NotificationTriggerEvent;
import com.meetpgandhi.edots.eventengine.model.RescheduleTriggerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("local")
public class LocalEventBus {

    private static final Logger log = LoggerFactory.getLogger(LocalEventBus.class);

    private final ObjectProvider<AuditLogConsumer> auditLogConsumerProvider;
    private final ObjectProvider<AutoRescheduleConsumer> autoRescheduleConsumerProvider;
    private final ObjectProvider<LocalNotificationHandler> notificationHandlerProvider;

    public LocalEventBus(
        ObjectProvider<AuditLogConsumer> auditLogConsumerProvider,
        ObjectProvider<AutoRescheduleConsumer> autoRescheduleConsumerProvider,
        ObjectProvider<LocalNotificationHandler> notificationHandlerProvider
    ) {
        this.auditLogConsumerProvider = auditLogConsumerProvider;
        this.autoRescheduleConsumerProvider = autoRescheduleConsumerProvider;
        this.notificationHandlerProvider = notificationHandlerProvider;
    }

    public void publishOrderEvent(
        OrderEvent event,
        String customerEmail,
        String customerPhone,
        String agentName,
        int currentAttemptCount,
        int maxAttempts
    ) {
        log.info("[LocalEventBus] Processing OrderEvent synchronously: orderRef={}, stage={}",
            event.orderReference(), event.newStage());

        // 1. Order events -> NotificationConsumer
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
        publishNotification(notifyEvent);

        // 2. Order events -> AuditLogConsumer
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
        publishAuditEvent(auditEvent);

        // 3. Reschedule triggers -> AutoRescheduleConsumer (if FAILED_ATTEMPT)
        if (event.newStage() == Stage.FAILED_ATTEMPT) {
            RescheduleTriggerEvent rescheduleEvent = new RescheduleTriggerEvent(
                event.orderId(),
                event.orderReference(),
                currentAttemptCount,
                maxAttempts,
                event.reasonCode(),
                Instant.now()
            );
            publishReschedule(rescheduleEvent);
        }
    }

    public void publishAuditEvent(AuditLogEvent auditEvent) {
        try {
            AuditLogConsumer consumer = auditLogConsumerProvider.getIfAvailable();
            if (consumer != null) {
                log.info("[LocalEventBus] Direct invocation of AuditLogConsumer for eventType={}", auditEvent.eventType());
                consumer.consumeAuditLog(auditEvent);
            } else {
                log.warn("[LocalEventBus] AuditLogConsumer not available");
            }
        } catch (Exception ex) {
            log.error("[LocalEventBus] Failed to process audit log event: {}", ex.getMessage(), ex);
        }
    }

    public void publishNotification(NotificationTriggerEvent notifyEvent) {
        try {
            LocalNotificationHandler handler = notificationHandlerProvider.getIfAvailable();
            if (handler != null) {
                log.info("[LocalEventBus] Direct invocation of NotificationConsumer for orderRef={}", notifyEvent.orderReference());
                handler.handleNotification(notifyEvent);
            } else {
                log.warn("[LocalEventBus] No LocalNotificationHandler available in context");
            }
        } catch (Exception ex) {
            log.error("[LocalEventBus] Failed to process notification trigger: {}", ex.getMessage(), ex);
        }
    }

    public void publishReschedule(RescheduleTriggerEvent rescheduleEvent) {
        try {
            AutoRescheduleConsumer consumer = autoRescheduleConsumerProvider.getIfAvailable();
            if (consumer != null) {
                log.info("[LocalEventBus] Direct invocation of AutoRescheduleConsumer for orderRef={}", rescheduleEvent.orderReference());
                consumer.processAutoReschedule(rescheduleEvent);
            } else {
                log.warn("[LocalEventBus] AutoRescheduleConsumer not available");
            }
        } catch (Exception ex) {
            log.error("[LocalEventBus] Failed to process auto-reschedule: {}", ex.getMessage(), ex);
        }
    }
}
