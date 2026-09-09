package com.meetpgandhi.edots.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.api.dto.AdminDtos.NotificationRuleRequest;
import com.meetpgandhi.edots.api.dto.AdminDtos.NotificationRuleResponse;
import com.meetpgandhi.edots.domain.entity.AdminUserEntity;
import com.meetpgandhi.edots.domain.entity.NotificationRuleEntity;
import com.meetpgandhi.edots.domain.exception.ResourceNotFoundException;
import com.meetpgandhi.edots.domain.model.ActorType;
import com.meetpgandhi.edots.domain.repository.AdminUserRepository;
import com.meetpgandhi.edots.domain.repository.NotificationRuleRepository;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications/rules")
@Tag(name = "Administrator Notification Rules", description = "Endpoints to configure event-driven notification channels and templates")
@SecurityRequirement(name = "BearerAuth")
public class AdminNotificationRuleController {

    private final NotificationRuleRepository ruleRepository;
    private final AdminUserRepository adminUserRepository;
    private final OrderEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public AdminNotificationRuleController(
        NotificationRuleRepository ruleRepository,
        AdminUserRepository adminUserRepository,
        OrderEventProducer eventProducer,
        ObjectMapper objectMapper
    ) {
        this.ruleRepository = ruleRepository;
        this.adminUserRepository = adminUserRepository;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "List all notification rules")
    public ResponseEntity<List<NotificationRuleResponse>> getRules() {
        List<NotificationRuleEntity> rules = ruleRepository.findAllByOrderByCreatedAtDesc();
        List<NotificationRuleResponse> response = rules.stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new notification rule")
    public ResponseEntity<NotificationRuleResponse> createRule(
        @Valid @RequestBody NotificationRuleRequest request,
        Principal principal
    ) {
        AdminUserEntity admin = adminUserRepository.findByEmail(principal.getName()).orElse(null);

        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setEventType(request.eventType().trim().toUpperCase());
        rule.setChannels(serializeChannels(request.channels()));
        rule.setTemplateBody(request.templateBody());
        rule.setActive(request.active() != null ? request.active() : true);
        rule.setCreatedBy(admin);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());

        NotificationRuleEntity saved = ruleRepository.save(rule);

        eventProducer.publishAuditEvent(AuditLogEvent.of(
            "NOTIFICATION_RULE_CREATED", principal.getName(), ActorType.ADMIN,
            String.valueOf(saved.getId()), "NOTIFICATION_RULE",
            String.format("Created rule for event %s with channels %s", saved.getEventType(), saved.getChannels())
        ));

        return ResponseEntity.ok(mapToResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing notification rule")
    public ResponseEntity<NotificationRuleResponse> updateRule(
        @PathVariable Long id,
        @Valid @RequestBody NotificationRuleRequest request,
        Principal principal
    ) {
        NotificationRuleEntity rule = ruleRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.rule(id));

        if (request.eventType() != null) rule.setEventType(request.eventType().trim().toUpperCase());
        if (request.channels() != null) rule.setChannels(serializeChannels(request.channels()));
        if (request.templateBody() != null) rule.setTemplateBody(request.templateBody());
        if (request.active() != null) rule.setActive(request.active());
        rule.setUpdatedAt(LocalDateTime.now());

        NotificationRuleEntity saved = ruleRepository.save(rule);

        eventProducer.publishAuditEvent(AuditLogEvent.of(
            "NOTIFICATION_RULE_UPDATED", principal.getName(), ActorType.ADMIN,
            String.valueOf(saved.getId()), "NOTIFICATION_RULE",
            String.format("Updated rule %d: active=%b, channels=%s", saved.getId(), saved.isActive(), saved.getChannels())
        ));

        return ResponseEntity.ok(mapToResponse(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a notification rule")
    public ResponseEntity<Void> deactivateRule(@PathVariable Long id, Principal principal) {
        NotificationRuleEntity rule = ruleRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.rule(id));

        rule.setActive(false);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(rule);

        eventProducer.publishAuditEvent(AuditLogEvent.of(
            "NOTIFICATION_RULE_DEACTIVATED", principal.getName(), ActorType.ADMIN,
            String.valueOf(id), "NOTIFICATION_RULE", "Deactivated notification rule " + id
        ));

        return ResponseEntity.noContent().build();
    }

    private NotificationRuleResponse mapToResponse(NotificationRuleEntity entity) {
        List<String> channels = parseChannels(entity.getChannels());
        return new NotificationRuleResponse(
            entity.getId(),
            entity.getEventType(),
            channels,
            entity.getTemplateBody(),
            entity.isActive(),
            entity.getUpdatedAt()
        );
    }

    private String serializeChannels(List<String> channels) {
        try {
            if (channels == null || channels.isEmpty()) return "[\"EMAIL\"]";
            return objectMapper.writeValueAsString(channels);
        } catch (Exception e) {
            return "[\"EMAIL\"]";
        }
    }

    private List<String> parseChannels(String channelsJson) {
        try {
            if (channelsJson == null) return Collections.emptyList();
            return objectMapper.readValue(channelsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("EMAIL");
        }
    }
}
