package com.meetpgandhi.edots.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.NotificationLogEntity;
import com.meetpgandhi.edots.domain.entity.NotificationRuleEntity;
import com.meetpgandhi.edots.domain.repository.NotificationLogRepository;
import com.meetpgandhi.edots.domain.repository.NotificationRuleRepository;
import com.meetpgandhi.edots.eventengine.local.LocalNotificationHandler;
import com.meetpgandhi.edots.eventengine.model.NotificationTriggerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationConsumer implements LocalNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRuleRepository ruleRepository;
    private final NotificationLogRepository logRepository;
    private final ObjectMapper objectMapper;

    @Value("${edots.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public NotificationConsumer(
        NotificationRuleRepository ruleRepository,
        NotificationLogRepository logRepository,
        ObjectMapper objectMapper
    ) {
        this.ruleRepository = ruleRepository;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${edots.kafka.topics.notification-trigger:edots.notification.trigger}",
        groupId = "edots-notification-group",
        containerFactory = "kafkaListenerContainerFactory",
        autoStartup = "${edots.kafka.enabled:true}"
    )
    public void consumeNotificationTrigger(Object record) {
        try {
            NotificationTriggerEvent event;
            if (record instanceof NotificationTriggerEvent nte) {
                event = nte;
            } else {
                event = objectMapper.convertValue(record, NotificationTriggerEvent.class);
            }

            processNotification(event);
        } catch (Exception ex) {
            log.error("Failed to process notification trigger: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void handleNotification(NotificationTriggerEvent event) {
        processNotification(event);
    }

    public void processNotification(NotificationTriggerEvent event) {
        String eventType = event.newStage().name();
        List<NotificationRuleEntity> activeRules = ruleRepository.findByEventTypeAndActiveTrue(eventType);

        String deepLinkUrl = String.format("%s/track/%s", frontendBaseUrl.replaceAll("/$", ""), event.orderReference());

        if (activeRules.isEmpty()) {
            // Default rule fallback: dispatch EMAIL simulation
            dispatchChannelSimulation("EMAIL", event, deepLinkUrl, "Status update for order " + event.orderReference());
            return;
        }

        for (NotificationRuleEntity rule : activeRules) {
            List<String> channels = parseChannels(rule.getChannels());
            for (String channel : channels) {
                String body = renderTemplate(rule.getTemplateBody(), event, deepLinkUrl);
                dispatchChannelSimulation(channel, event, deepLinkUrl, body);
            }
        }
    }

    private void dispatchChannelSimulation(
        String channel,
        NotificationTriggerEvent event,
        String deepLink,
        String messageBody
    ) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("orderReference", event.orderReference());
            payloadMap.put("stage", event.newStage().name());
            payloadMap.put("customerEmail", event.customerEmail());
            payloadMap.put("customerPhone", event.customerPhone());
            payloadMap.put("agentName", event.agentName());
            payloadMap.put("deepLinkUrl", deepLink);
            payloadMap.put("message", messageBody);
            payloadMap.put("channel", channel);

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            NotificationLogEntity logEntry = new NotificationLogEntity();
            logEntry.setOrderId(event.orderId());
            logEntry.setCustomerEmail(event.customerEmail());
            logEntry.setChannel(channel);
            logEntry.setEventType(event.newStage().name());
            logEntry.setPayload(jsonPayload);
            logEntry.setStatus("SENT");
            logEntry.setSentAt(LocalDateTime.now());

            logRepository.save(logEntry);

            log.info("📢 [SIMULATED {} NOTIFICATION] To: {} | Order: {} | Deep-link: {} | Body: '{}'",
                channel, event.customerEmail(), event.orderReference(), deepLink, messageBody);

        } catch (Exception ex) {
            log.error("Failed to dispatch {} notification for order {}: {}",
                channel, event.orderReference(), ex.getMessage(), ex);
        }
    }

    private List<String> parseChannels(String channelsJson) {
        try {
            if (channelsJson == null || channelsJson.trim().isEmpty()) {
                return List.of("EMAIL");
            }
            return objectMapper.readValue(channelsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("EMAIL");
        }
    }

    private String renderTemplate(String template, NotificationTriggerEvent event, String deepLink) {
        if (template == null || template.trim().isEmpty()) {
            return String.format("Order %s updated to %s. Track: %s",
                event.orderReference(), event.newStage().getDisplayName(), deepLink);
        }
        return template
            .replace("{{orderReference}}", event.orderReference() != null ? event.orderReference() : "")
            .replace("{{stage}}", event.newStage() != null ? event.newStage().getDisplayName() : "")
            .replace("{{agentName}}", event.agentName() != null ? event.agentName() : "")
            .replace("{{reasonCode}}", event.reasonCode() != null ? event.reasonCode() : "")
            .replace("{{deepLink}}", deepLink);
    }
}
