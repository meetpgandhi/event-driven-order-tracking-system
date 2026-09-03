package com.meetpgandhi.edots.domain.entity;

import com.meetpgandhi.edots.domain.model.ActorType;
import com.meetpgandhi.edots.domain.model.Stage;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_events", indexes = {
    @Index(name = "idx_order_events_order_time", columnList = "order_id, created_at")
})
public class OrderEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_stage", length = 30)
    private Stage previousStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_stage", nullable = false, length = 30)
    private Stage newStage;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        throw new UnsupportedOperationException("Order events are immutable and cannot be updated.");
    }

    @PreRemove
    protected void onDelete() {
        throw new UnsupportedOperationException("Order events are immutable and cannot be deleted.");
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Stage getPreviousStage() { return previousStage; }
    public void setPreviousStage(Stage previousStage) { this.previousStage = previousStage; }

    public Stage getNewStage() { return newStage; }
    public void setNewStage(Stage newStage) { this.newStage = newStage; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public ActorType getActorType() { return actorType; }
    public void setActorType(ActorType actorType) { this.actorType = actorType; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
