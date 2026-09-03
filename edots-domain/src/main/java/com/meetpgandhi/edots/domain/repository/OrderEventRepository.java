package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEventEntity, Long> {

    List<OrderEventEntity> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    List<OrderEventEntity> findByOrder_OrderReferenceOrderByCreatedAtAsc(String orderReference);

    long countByCreatedAtAfter(LocalDateTime timestamp);
}
