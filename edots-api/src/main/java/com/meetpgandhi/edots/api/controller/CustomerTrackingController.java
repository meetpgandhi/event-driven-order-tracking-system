package com.meetpgandhi.edots.api.controller;

import com.meetpgandhi.edots.api.dto.TrackingDtos.CustomerTrackingResponse;
import com.meetpgandhi.edots.api.dto.TrackingDtos.EventTimelineItem;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OrderEventEntity;
import com.meetpgandhi.edots.domain.exception.ResourceNotFoundException;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.OrderEventRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Customer Tracking", description = "Public endpoints for self-service order tracking (No authentication required)")
public class CustomerTrackingController {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;

    public CustomerTrackingController(
        OrderRepository orderRepository,
        OrderEventRepository orderEventRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @GetMapping("/{orderReference}")
    @Operation(summary = "Get current order status and complete timeline history by reference")
    public ResponseEntity<CustomerTrackingResponse> getOrderTracking(@PathVariable String orderReference) {
        OrderEntity order = orderRepository.findByOrderReference(orderReference.trim().toUpperCase())
            .orElseThrow(() -> ResourceNotFoundException.order(orderReference));

        List<OrderEventEntity> events = orderEventRepository
            .findByOrder_OrderReferenceOrderByCreatedAtAsc(order.getOrderReference());

        List<EventTimelineItem> timeline = events.stream().map(this::mapTimelineItem).toList();

        // Agent name is exposed only during delivery stage or if assigned
        String assignedAgentName = null;
        if (order.getAssignedAgent() != null && (order.getCurrentStage() == Stage.OUT_FOR_DELIVERY || order.getCurrentStage() == Stage.DELIVERED)) {
            assignedAgentName = order.getAssignedAgent().getName();
        }

        CustomerTrackingResponse response = new CustomerTrackingResponse(
            order.getId(),
            order.getOrderReference(),
            order.getCustomerName(),
            order.getCurrentStage().name(),
            order.getCurrentStage().getDisplayName(),
            assignedAgentName,
            order.getCarrierId(),
            order.getAttemptCount(),
            order.getMaxAttempts(),
            order.getNextDeliverySlot(),
            order.getUpdatedAt(),
            timeline
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderReference}/events")
    @Operation(summary = "Get chronological event history timeline for an order")
    public ResponseEntity<List<EventTimelineItem>> getOrderEvents(@PathVariable String orderReference) {
        List<OrderEventEntity> events = orderEventRepository
            .findByOrder_OrderReferenceOrderByCreatedAtAsc(orderReference.trim().toUpperCase());

        if (events.isEmpty() && orderRepository.findByOrderReference(orderReference.trim().toUpperCase()).isEmpty()) {
            throw ResourceNotFoundException.order(orderReference);
        }

        List<EventTimelineItem> timeline = events.stream().map(this::mapTimelineItem).toList();
        return ResponseEntity.ok(timeline);
    }

    private EventTimelineItem mapTimelineItem(OrderEventEntity entity) {
        String prevStage = entity.getPreviousStage() != null ? entity.getPreviousStage().name() : null;
        String newStageDisplay = entity.getNewStage() != null ? entity.getNewStage().getDisplayName() : entity.getEventType();

        return new EventTimelineItem(
            entity.getId(),
            entity.getEventType(),
            prevStage,
            entity.getNewStage().name(),
            newStageDisplay,
            entity.getActorId(),
            entity.getActorType().name(),
            entity.getReasonCode(),
            entity.getCreatedAt()
        );
    }
}
