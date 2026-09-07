package com.meetpgandhi.edots.api.controller;

import com.meetpgandhi.edots.api.dto.AgentDtos.*;
import com.meetpgandhi.edots.api.service.OrderManagementService;
import com.meetpgandhi.edots.api.service.OtpService;
import com.meetpgandhi.edots.domain.entity.DeliveryAgentEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.exception.ResourceNotFoundException;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.DeliveryAgentRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.statemachine.OrderStateMachine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agent/orders")
@Tag(name = "Delivery Agent", description = "Endpoints for delivery agents to view and update orders")
@SecurityRequirement(name = "BearerAuth")
public class AgentOrderController {

    private final OrderRepository orderRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final OrderManagementService orderManagementService;
    private final OtpService otpService;
    private final OrderStateMachine stateMachine;

    public AgentOrderController(
        OrderRepository orderRepository,
        DeliveryAgentRepository deliveryAgentRepository,
        OrderManagementService orderManagementService,
        OtpService otpService,
        OrderStateMachine stateMachine
    ) {
        this.orderRepository = orderRepository;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.orderManagementService = orderManagementService;
        this.otpService = otpService;
        this.stateMachine = stateMachine;
    }

    @GetMapping
    @Operation(summary = "Get list of orders assigned to authenticated agent")
    public ResponseEntity<List<AgentOrderResponse>> getAssignedOrders(Principal principal) {
        DeliveryAgentEntity agent = deliveryAgentRepository.findByEmail(principal.getName())
            .orElseThrow(() -> ResourceNotFoundException.agent(principal.getName()));

        List<OrderEntity> orders = orderRepository.findByAssignedAgent_Id(agent.getId());

        List<AgentOrderResponse> response = orders.stream().map(order -> {
            Set<Stage> nextStages = stateMachine.getNextValidStages(order.getCurrentStage());
            List<String> nextStageNames = nextStages.stream().map(Enum::name).toList();

            return new AgentOrderResponse(
                order.getId(),
                order.getOrderReference(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getCurrentStage().name(),
                order.getAttemptCount(),
                order.getMaxAttempts(),
                order.getNextDeliverySlot(),
                nextStageNames
            );
        }).toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/transition")
    @Operation(summary = "Submit a stage transition for an assigned order")
    public ResponseEntity<OrderEvent> transitionOrder(
        @PathVariable Long id,
        @Valid @RequestBody StageTransitionRequest request,
        Principal principal
    ) {
        OrderEvent event = orderManagementService.handleAgentTransition(id, principal.getName(), request);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/{id}/otp/generate")
    @Operation(summary = "Generate a 6-digit delivery confirmation OTP (10 minute expiry)")
    public ResponseEntity<OtpGenerateResponse> generateOtp(
        @PathVariable Long id,
        Principal principal
    ) {
        OtpGenerateResponse response = otpService.generateOtp(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/otp/verify")
    @Operation(summary = "Verify OTP code submitted by customer to confirm delivery")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(
        @PathVariable Long id,
        @Valid @RequestBody OtpVerifyRequest request
    ) {
        OtpVerifyResponse response = otpService.verifyOtp(id, request.otpCode());
        return ResponseEntity.ok(response);
    }
}
