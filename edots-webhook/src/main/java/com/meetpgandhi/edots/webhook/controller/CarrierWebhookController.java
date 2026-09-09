package com.meetpgandhi.edots.webhook.controller;

import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.webhook.dto.CarrierWebhookPayload;
import com.meetpgandhi.edots.webhook.service.CarrierWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/carrier")
@Tag(name = "Carrier Webhooks", description = "Inbound endpoints for third-party courier webhook status updates")
public class CarrierWebhookController {

    private final CarrierWebhookService webhookService;

    public CarrierWebhookController(CarrierWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{carrierId}")
    @Operation(
        summary = "Receive inbound carrier status event",
        description = "Requires pre-shared API key in X-Api-Key header. Validates payload, maps to EDOTS stage, and publishes native tracking event."
    )
    public ResponseEntity<OrderEvent> receiveCarrierWebhook(
        @PathVariable String carrierId,
        @Parameter(in = ParameterIn.HEADER, name = "X-Api-Key", required = true, description = "Carrier authentication API key")
        @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
        @Valid @RequestBody CarrierWebhookPayload payload
    ) {
        OrderEvent event = webhookService.processWebhook(carrierId, apiKey, payload);
        return ResponseEntity.ok(event);
    }
}
