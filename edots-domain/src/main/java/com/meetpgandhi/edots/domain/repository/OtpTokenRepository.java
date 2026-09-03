package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.OtpTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpTokenEntity, Long> {
    Optional<OtpTokenEntity> findTopByOrderIdAndAgentIdOrderByCreatedAtDesc(Long orderId, Long agentId);
    Optional<OtpTokenEntity> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);
}
