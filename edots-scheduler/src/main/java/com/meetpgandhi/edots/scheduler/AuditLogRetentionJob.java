package com.meetpgandhi.edots.scheduler;

import com.meetpgandhi.edots.domain.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AuditLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionJob.class);

    private final AuditLogRepository auditLogRepository;

    @Value("${edots.scheduling.retention-days:90}")
    private int retentionDays;

    public AuditLogRetentionJob(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(cron = "${edots.scheduling.retention-cron:0 0 2 * * ?}")
    @Transactional
    public int runRetentionCleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("Executing AuditLogRetentionJob: purging audit records older than {} days (cutoff: {})",
            retentionDays, cutoff);

        int deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoff);
        log.info("AuditLogRetentionJob completed: purged {} expired audit records", deletedCount);
        return deletedCount;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
