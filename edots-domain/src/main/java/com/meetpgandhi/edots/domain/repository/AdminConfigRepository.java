package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.AdminConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminConfigRepository extends JpaRepository<AdminConfigEntity, Long> {
    Optional<AdminConfigEntity> findByConfigKey(String configKey);
}
