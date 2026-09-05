package com.meetpgandhi.edots.eventengine.local;

import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile("local")
public class LocalOrderEventProducer extends OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(LocalOrderEventProducer.class);

    private final LocalEventBus localEventBus;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    public LocalOrderEventProducer(
        LocalEventBus localEventBus,
        ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider
    ) {
        super(null);
        this.localEventBus = localEventBus;
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

    @Override
    public void publishOrderEvent(
        OrderEvent event,
        String customerEmail,
        String customerPhone,
        String agentName,
        int currentAttemptCount,
        int maxAttempts
    ) {
        log.info("[LocalOrderEventProducer] Dispatching order event: orderRef={}, stage={}",
            event.orderReference(), event.newStage());

        // 1. Invoke synchronous local event bus (Notifications, Audit, Auto-Reschedule)
        localEventBus.publishOrderEvent(event, customerEmail, customerPhone, agentName, currentAttemptCount, maxAttempts);

        // 2. Broadcast to STOMP WebSocket broker for admin dashboard live updates
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/dashboard-events", event);
                log.info("[LocalOrderEventProducer] Broadcast event to /topic/dashboard-events for orderRef={}",
                    event.orderReference());
            } catch (Exception ex) {
                log.warn("[LocalOrderEventProducer] Failed to broadcast event via WebSocket: {}", ex.getMessage());
            }
        }
    }

    @Override
    public void publishAuditEvent(AuditLogEvent auditEvent) {
        log.info("[LocalOrderEventProducer] Dispatching audit event: eventType={}, ref={}",
            auditEvent.eventType(), auditEvent.referenceId());
        localEventBus.publishAuditEvent(auditEvent);
    }
}
