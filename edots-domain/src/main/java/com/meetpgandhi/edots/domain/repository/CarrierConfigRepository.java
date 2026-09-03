package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.CarrierConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarrierConfigRepository extends JpaRepository<CarrierConfigEntity, Long> {
    Optional<CarrierConfigEntity> findByCarrierIdAndActiveTrue(String carrierId);
    Optional<CarrierConfigEntity> findByCarrierId(String carrierId);
}
