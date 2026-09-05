package com.meetpgandhi.edots.eventengine.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.AuditLogEntity;
import com.meetpgandhi.edots.domain.model.ActorType;
import com.meetpgandhi.edots.domain.repository.AuditLogRepository;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogConsumerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditLogConsumer consumer;

    @Captor
    private ArgumentCaptor<AuditLogEntity> entityCaptor;

    @BeforeEach
    void setUp() {
        consumer = new AuditLogConsumer(auditLogRepository, objectMapper);
    }

    @Test
    @DisplayName("Consuming AuditLogEvent saves AuditLogEntity to repository")
    void shouldPersistAuditLogEntity() {
        AuditLogEvent event = new AuditLogEvent(
            "STAGE_CHANGE", "agent1@edots.dev", ActorType.AGENT,
            "ORD-001", "ORDER", "Stage transitioned to DELIVERED", Instant.now()
        );

        consumer.consumeAuditLog(event);

        verify(auditLogRepository).save(entityCaptor.capture());
        AuditLogEntity saved = entityCaptor.getValue();
        assertThat(saved.getEventType()).isEqualTo("STAGE_CHANGE");
        assertThat(saved.getActorId()).isEqualTo("agent1@edots.dev");
        assertThat(saved.getActorType()).isEqualTo(ActorType.AGENT);
        assertThat(saved.getReferenceId()).isEqualTo("ORD-001");
        assertThat(saved.getReferenceType()).isEqualTo("ORDER");
        assertThat(saved.getDetail()).isEqualTo("Stage transitioned to DELIVERED");
    }
}
