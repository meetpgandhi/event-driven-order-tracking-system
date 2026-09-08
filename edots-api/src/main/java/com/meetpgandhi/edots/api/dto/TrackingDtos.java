package com.meetpgandhi.edots.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TrackingDtos {

    public record CustomerTrackingResponse(
        Long orderId,
        String orderReference,
        String customerName,
        String currentStage,
        String currentStageDisplayName,
        String assignedAgentName,
        String carrierId,
        int attemptCount,
        int maxAttempts,
        LocalDateTime nextDeliverySlot,
        LocalDateTime updatedAt,
        List<EventTimelineItem> timeline
    ) {}

    public record EventTimelineItem(
        Long id,
        String eventType,
        String previousStage,
        String newStage,
        String newStageDisplayName,
        String actorId,
        String actorType,
        String reasonCode,
        LocalDateTime timestamp
    ) {}
}
