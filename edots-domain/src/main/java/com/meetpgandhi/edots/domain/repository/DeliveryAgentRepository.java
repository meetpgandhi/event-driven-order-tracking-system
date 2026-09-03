package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.DeliveryAgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgentEntity, Long> {
    Optional<DeliveryAgentEntity> findByEmail(String email);
    Optional<DeliveryAgentEntity> findByEmailAndActiveTrue(String email);
}
