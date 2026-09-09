package com.meetpgandhi.edots.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminDtos {

    public record DashboardStatsResponse(
        long totalOrders,
        Map<String, Long> ordersByStage,
        long staleOrdersCount,
        long todayEventsCount
    ) {}

    public record NotificationRuleRequest(
        @NotBlank(message = "Event type is required")
        String eventType,

        List<String> channels,

        String templateBody,

        Boolean active
    ) {}

    public record NotificationRuleResponse(
        Long id,
        String eventType,
        List<String> channels,
        String templateBody,
        boolean active,
        LocalDateTime updatedAt
    ) {}

    public record AdminOrderSummary(
        Long id,
        String orderReference,
        String customerName,
        String customerEmail,
        String customerPhone,
        String currentStage,
        String assignedAgentName,
        String carrierId,
        int attemptCount,
        int maxAttempts,
        LocalDateTime nextDeliverySlot,
        boolean isStale,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
}
