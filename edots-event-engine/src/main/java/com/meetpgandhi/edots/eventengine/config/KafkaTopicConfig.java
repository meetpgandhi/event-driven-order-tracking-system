package com.meetpgandhi.edots.eventengine.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!local")
public class KafkaTopicConfig {

    @Value("${edots.kafka.topics.order-events:edots.order.events}")
    private String orderEventsTopic;

    @Value("${edots.kafka.topics.notification-trigger:edots.notification.trigger}")
    private String notificationTriggerTopic;

    @Value("${edots.kafka.topics.audit-log:edots.audit.log}")
    private String auditLogTopic;

    @Value("${edots.kafka.topics.webhook-inbound:edots.webhook.inbound}")
    private String webhookInboundTopic;

    @Value("${edots.kafka.topics.reschedule-trigger:edots.reschedule.trigger}")
    private String rescheduleTriggerTopic;

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderEventsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic notificationTriggerTopic() {
        return TopicBuilder.name(notificationTriggerTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic auditLogTopic() {
        return TopicBuilder.name(auditLogTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic webhookInboundTopic() {
        return TopicBuilder.name(webhookInboundTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic rescheduleTriggerTopic() {
        return TopicBuilder.name(rescheduleTriggerTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }
}
