package com.meetpgandhi.edots.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CarrierWebhookPayload(
    @NotBlank(message = "orderReference is required")
    String orderReference,

    @NotBlank(message = "status is required")
    String status,

    String reasonCode,

    Map<String, Object> metadata
) {}
