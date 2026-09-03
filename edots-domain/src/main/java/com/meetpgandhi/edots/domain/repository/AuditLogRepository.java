package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByEventType(String eventType, Pageable pageable);

    Page<AuditLogEntity> findByReferenceId(String referenceId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.createdAt < :retentionThreshold")
    int deleteByCreatedAtBefore(@Param("retentionThreshold") LocalDateTime retentionThreshold);
}
