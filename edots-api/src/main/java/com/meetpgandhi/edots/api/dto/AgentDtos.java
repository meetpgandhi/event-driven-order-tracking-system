package com.meetpgandhi.edots.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AgentDtos {

    public record StageTransitionRequest(
        @NotBlank(message = "New stage is required")
        String newStage,

        String reasonCode,

        Map<String, Object> metadata
    ) {}

    public record OtpGenerateResponse(
        Long orderId,
        String message,
        LocalDateTime expiresAt,
        String debugOtp // included in MVP simulation so tester can easily verify OTP
    ) {}

    public record OtpVerifyRequest(
        @NotBlank(message = "OTP code is required")
        String otpCode
    ) {}

    public record OtpVerifyResponse(
        Long orderId,
        boolean verified,
        String message
    ) {}

    public record AgentOrderResponse(
        Long id,
        String orderReference,
        String customerName,
        String customerPhone,
        String customerEmail,
        String currentStage,
        int attemptCount,
        int maxAttempts,
        LocalDateTime nextDeliverySlot,
        List<String> availableNextStages
    ) {}
}
