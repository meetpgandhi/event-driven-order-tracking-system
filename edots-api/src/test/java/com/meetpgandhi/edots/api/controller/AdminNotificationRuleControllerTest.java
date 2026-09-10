package com.meetpgandhi.edots.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.api.dto.AdminDtos.NotificationRuleRequest;
import com.meetpgandhi.edots.api.dto.AdminDtos.NotificationRuleResponse;
import com.meetpgandhi.edots.domain.entity.AdminUserEntity;
import com.meetpgandhi.edots.domain.entity.NotificationRuleEntity;
import com.meetpgandhi.edots.domain.repository.AdminUserRepository;
import com.meetpgandhi.edots.domain.repository.NotificationRuleRepository;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminNotificationRuleControllerTest {

    @Mock
    private NotificationRuleRepository ruleRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private OrderEventProducer eventProducer;

    @Mock
    private Principal principal;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AdminNotificationRuleController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminNotificationRuleController(
            ruleRepository, adminUserRepository, eventProducer, objectMapper
        );
        lenient().when(principal.getName()).thenReturn("admin@edots.dev");
    }

    @Test
    @DisplayName("Create notification rule saves entity and publishes audit event")
    void shouldCreateNotificationRule() {
        AdminUserEntity admin = new AdminUserEntity();
        admin.setEmail("admin@edots.dev");
        when(adminUserRepository.findByEmail("admin@edots.dev")).thenReturn(Optional.of(admin));

        NotificationRuleEntity saved = new NotificationRuleEntity();
        saved.setId(10L);
        saved.setEventType("DISPATCHED");
        saved.setChannels("[\"EMAIL\",\"SMS\"]");
        saved.setTemplateBody("Order {{orderReference}} dispatched");
        saved.setActive(true);

        when(ruleRepository.save(any(NotificationRuleEntity.class))).thenReturn(saved);

        NotificationRuleRequest request = new NotificationRuleRequest(
            "DISPATCHED", List.of("EMAIL", "SMS"), "Order {{orderReference}} dispatched", true
        );

        ResponseEntity<NotificationRuleResponse> response = controller.createRule(request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().eventType()).isEqualTo("DISPATCHED");
        verify(eventProducer).publishAuditEvent(any());
    }

    @Test
    @DisplayName("Deactivate rule sets active=false and logs audit trail")
    void shouldDeactivateNotificationRule() {
        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setId(5L);
        rule.setActive(true);

        when(ruleRepository.findById(5L)).thenReturn(Optional.of(rule));

        ResponseEntity<Void> response = controller.deactivateRule(5L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(rule.isActive()).isFalse();
        verify(ruleRepository).save(rule);
        verify(eventProducer).publishAuditEvent(any());
    }
}
