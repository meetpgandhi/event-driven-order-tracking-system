package com.meetpgandhi.edots.scheduler;

import com.meetpgandhi.edots.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionJobTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogRetentionJob retentionJob;

    @BeforeEach
    void setUp() {
        retentionJob = new AuditLogRetentionJob(auditLogRepository);
        retentionJob.setRetentionDays(90);
    }

    @Test
    @DisplayName("Retention cleanup purges audit records older than configured threshold")
    void shouldPurgeExpiredAuditRecords() {
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(15);

        int purgedCount = retentionJob.runRetentionCleanup();

        assertThat(purgedCount).isEqualTo(15);
        verify(auditLogRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
