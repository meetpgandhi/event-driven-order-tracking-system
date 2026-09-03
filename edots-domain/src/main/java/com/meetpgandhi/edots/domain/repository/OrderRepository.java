package com.meetpgandhi.edots.domain.repository;

import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.model.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    Optional<OrderEntity> findByOrderReference(String orderReference);

    List<OrderEntity> findByAssignedAgent_Id(Long agentId);

    List<OrderEntity> findByAssignedAgent_IdAndCurrentStageIn(Long agentId, List<Stage> stages);

    @Query("SELECT o FROM OrderEntity o WHERE o.updatedAt < :threshold AND o.currentStage NOT IN (:terminalStages)")
    List<OrderEntity> findStaleOrders(
        @Param("threshold") LocalDateTime threshold,
        @Param("terminalStages") List<Stage> terminalStages
    );

    long countByCurrentStage(Stage stage);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.updatedAt < :threshold AND o.currentStage NOT IN (:terminalStages)")
    long countStaleOrders(
        @Param("threshold") LocalDateTime threshold,
        @Param("terminalStages") List<Stage> terminalStages
    );
}
