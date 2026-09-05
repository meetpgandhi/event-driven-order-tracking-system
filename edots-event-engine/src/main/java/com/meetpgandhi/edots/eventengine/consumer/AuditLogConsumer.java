package com.meetpgandhi.edots.eventengine.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.AuditLogEntity;
import com.meetpgandhi.edots.domain.repository.AuditLogRepository;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogConsumer(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${edots.kafka.topics.audit-log:edots.audit.log}",
        groupId = "edots-audit-group",
        containerFactory = "kafkaListenerContainerFactory",
        autoStartup = "${edots.kafka.enabled:true}"
    )
    public void consumeAuditLog(Object record) {
        try {
            AuditLogEvent event;
            if (record instanceof AuditLogEvent ale) {
                event = ale;
            } else {
                event = objectMapper.convertValue(record, AuditLogEvent.class);
            }

            AuditLogEntity entity = new AuditLogEntity();
            entity.setEventType(event.eventType());
            entity.setActorId(event.actorId());
            entity.setActorType(event.actorType());
            entity.setReferenceId(event.referenceId());
            entity.setReferenceType(event.referenceType());
            entity.setDetail(event.detail());
            entity.setCreatedAt(LocalDateTime.now());

            auditLogRepository.save(entity);
            log.info("Persisted immutable audit log entry: id={}, eventType={}, ref={}",
                entity.getId(), entity.getEventType(), entity.getReferenceId());

        } catch (Exception ex) {
            log.error("Failed to process audit log event: {}", ex.getMessage(), ex);
        }
    }
}
