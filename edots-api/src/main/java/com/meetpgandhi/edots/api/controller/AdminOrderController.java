package com.meetpgandhi.edots.api.controller;

import com.meetpgandhi.edots.api.dto.AdminDtos.AdminOrderSummary;
import com.meetpgandhi.edots.api.dto.AdminDtos.DashboardStatsResponse;
import com.meetpgandhi.edots.domain.entity.AuditLogEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.AuditLogRepository;
import com.meetpgandhi.edots.domain.repository.OrderEventRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administrator Operations", description = "Endpoints for administrator live queue, stale monitoring, stats, and audit log")
@SecurityRequirement(name = "BearerAuth")
public class AdminOrderController {

    private static final List<Stage> TERMINAL_STAGES = List.of(Stage.DELIVERED, Stage.RETURNED);

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminOrderController(
        OrderRepository orderRepository,
        OrderEventRepository orderEventRepository,
        AuditLogRepository auditLogRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/orders")
    @Operation(summary = "Get paginated, filterable fulfillment order queue")
    public ResponseEntity<Page<AdminOrderSummary>> getOrders(
        @RequestParam(required = false) Stage stage,
        @RequestParam(required = false) String carrier,
        @RequestParam(required = false) Long agentId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        LocalDateTime staleThreshold = LocalDateTime.now().minusHours(48);

        Page<OrderEntity> orderPage;
        if (stage != null) {
            orderPage = orderRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("currentStage"), stage),
                pageable
            );
        } else if (carrier != null && !carrier.isBlank()) {
            orderPage = orderRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("carrierId"), carrier.trim().toLowerCase()),
                pageable
            );
        } else if (agentId != null) {
            orderPage = orderRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("assignedAgent").get("id"), agentId),
                pageable
            );
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        Page<AdminOrderSummary> result = orderPage.map(o -> mapToSummary(o, staleThreshold));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/orders/stale")
    @Operation(summary = "Get active orders with no activity for 48+ hours")
    public ResponseEntity<List<AdminOrderSummary>> getStaleOrders() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusHours(48);
        List<OrderEntity> staleList = orderRepository.findStaleOrders(staleThreshold, TERMINAL_STAGES);
        List<AdminOrderSummary> summaries = staleList.stream()
            .map(o -> mapToSummary(o, staleThreshold))
            .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get aggregated KPI statistics and stage breakdown for admin dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        long totalOrders = orderRepository.count();
        LocalDateTime staleThreshold = LocalDateTime.now().minusHours(48);
        long staleCount = orderRepository.countStaleOrders(staleThreshold, TERMINAL_STAGES);

        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayEvents = orderEventRepository.countByCreatedAtAfter(startOfToday);

        Map<String, Long> stageCounts = new LinkedHashMap<>();
        for (Stage s : Stage.values()) {
            stageCounts.put(s.name(), orderRepository.countByCurrentStage(s));
        }

        DashboardStatsResponse stats = new DashboardStatsResponse(totalOrders, stageCounts, staleCount, todayEvents);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/audit-log")
    @Operation(summary = "Get paginated, filterable immutable audit log entries")
    public ResponseEntity<Page<AuditLogEntity>> getAuditLog(
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String referenceId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogEntity> logPage;

        if (eventType != null && !eventType.isBlank()) {
            logPage = auditLogRepository.findByEventType(eventType.trim(), pageable);
        } else if (referenceId != null && !referenceId.isBlank()) {
            logPage = auditLogRepository.findByReferenceId(referenceId.trim(), pageable);
        } else {
            logPage = auditLogRepository.findAll(pageable);
        }

        return ResponseEntity.ok(logPage);
    }

    private AdminOrderSummary mapToSummary(OrderEntity o, LocalDateTime staleThreshold) {
        boolean isStale = o.getUpdatedAt().isBefore(staleThreshold) && !TERMINAL_STAGES.contains(o.getCurrentStage());
        String agentName = o.getAssignedAgent() != null ? o.getAssignedAgent().getName() : null;

        return new AdminOrderSummary(
            o.getId(),
            o.getOrderReference(),
            o.getCustomerName(),
            o.getCustomerEmail(),
            o.getCustomerPhone(),
            o.getCurrentStage().name(),
            agentName,
            o.getCarrierId(),
            o.getAttemptCount(),
            o.getMaxAttempts(),
            o.getNextDeliverySlot(),
            isStale,
            o.getCreatedAt(),
            o.getUpdatedAt()
        );
    }
}
