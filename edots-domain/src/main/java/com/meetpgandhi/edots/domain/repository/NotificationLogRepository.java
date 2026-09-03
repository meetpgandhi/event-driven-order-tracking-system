package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {
    List<NotificationLogEntity> findByOrderIdOrderBySentAtDesc(Long orderId);
}
