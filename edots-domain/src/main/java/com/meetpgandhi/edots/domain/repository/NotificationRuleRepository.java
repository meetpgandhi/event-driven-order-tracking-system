package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.NotificationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, Long> {
    List<NotificationRuleEntity> findByEventTypeAndActiveTrue(String eventType);
    List<NotificationRuleEntity> findAllByOrderByCreatedAtDesc();
}
