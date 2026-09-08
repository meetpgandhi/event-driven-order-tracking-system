package com.meetpgandhi.edots.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.NotificationLogEntity;
import com.meetpgandhi.edots.domain.entity.NotificationRuleEntity;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.NotificationLogRepository;
import com.meetpgandhi.edots.domain.repository.NotificationRuleRepository;
import com.meetpgandhi.edots.eventengine.model.NotificationTriggerEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationRuleRepository ruleRepository;

    @Mock
    private NotificationLogRepository logRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NotificationConsumer consumer;

    @Captor
    private ArgumentCaptor<NotificationLogEntity> logCaptor;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(ruleRepository, logRepository, objectMapper);
        ReflectionTestUtils.setField(consumer, "frontendBaseUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("Process notification evaluates rules and creates notification_log records with deep-link")
    void shouldProcessNotificationAndCreateLogEntries() {
        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setEventType("OUT_FOR_DELIVERY");
        rule.setChannels("[\"EMAIL\", \"SMS\"]");
        rule.setTemplateBody("Order {{orderReference}} is out for delivery with {{agentName}}. Track: {{deepLink}}");

        when(ruleRepository.findByEventTypeAndActiveTrue("OUT_FOR_DELIVERY"))
            .thenReturn(List.of(rule));

        NotificationTriggerEvent event = new NotificationTriggerEvent(
            101L, "ORD-2026-9999", Stage.DISPATCHED, Stage.OUT_FOR_DELIVERY,
            "customer@example.com", "+919999999999", "Ramesh Kumar", null, Instant.now()
        );

        consumer.processNotification(event);

        verify(logRepository, times(2)).save(logCaptor.capture());
        List<NotificationLogEntity> savedLogs = logCaptor.getAllValues();

        assertThat(savedLogs).hasSize(2);
        assertThat(savedLogs).extracting(NotificationLogEntity::getChannel)
            .containsExactlyInAnyOrder("EMAIL", "SMS");

        NotificationLogEntity emailLog = savedLogs.stream()
            .filter(l -> l.getChannel().equals("EMAIL")).findFirst().orElseThrow();
        assertThat(emailLog.getPayload()).contains("http://localhost:5173/track/ORD-2026-9999");
        assertThat(emailLog.getPayload()).contains("Ramesh Kumar");
        assertThat(emailLog.getStatus()).isEqualTo("SENT");
    }

    @Test
    @DisplayName("Fallback to default EMAIL channel if no explicit rule configured")
    void shouldFallbackToDefaultChannelWhenNoRule() {
        when(ruleRepository.findByEventTypeAndActiveTrue("PICKING"))
            .thenReturn(List.of());

        NotificationTriggerEvent event = new NotificationTriggerEvent(
            102L, "ORD-2026-8888", Stage.ORDER_PLACED, Stage.PICKING,
            "customer2@example.com", null, null, null, Instant.now()
        );

        consumer.processNotification(event);

        verify(logRepository, times(1)).save(logCaptor.capture());
        NotificationLogEntity saved = logCaptor.getValue();
        assertThat(saved.getChannel()).isEqualTo("EMAIL");
        assertThat(saved.getPayload()).contains("http://localhost:5173/track/ORD-2026-8888");
    }
}
