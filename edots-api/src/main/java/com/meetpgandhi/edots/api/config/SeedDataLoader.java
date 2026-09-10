package com.meetpgandhi.edots.api.config;

import com.meetpgandhi.edots.domain.entity.*;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Component
public class SeedDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    private final AdminUserRepository adminUserRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final NotificationRuleRepository notificationRuleRepository;
    private final CarrierConfigRepository carrierConfigRepository;
    private final AdminConfigRepository adminConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataLoader(
        AdminUserRepository adminUserRepository,
        DeliveryAgentRepository deliveryAgentRepository,
        OrderRepository orderRepository,
        OrderEventRepository orderEventRepository,
        NotificationRuleRepository notificationRuleRepository,
        CarrierConfigRepository carrierConfigRepository,
        AdminConfigRepository adminConfigRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.adminUserRepository = adminUserRepository;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.notificationRuleRepository = notificationRuleRepository;
        this.carrierConfigRepository = carrierConfigRepository;
        this.adminConfigRepository = adminConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing/verifying EDOTS seed data...");

        // 1. Admin user: admin@edots.dev / admin123
        AdminUserEntity admin = adminUserRepository.findByEmail("admin@edots.dev").orElseGet(() -> {
            AdminUserEntity a = new AdminUserEntity();
            a.setEmail("admin@edots.dev");
            a.setName("EDOTS System Administrator");
            a.setRole("ADMIN");
            return a;
        });
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        adminUserRepository.save(admin);

        // 2. Agents: agent1@edots.dev and agent2@edots.dev / agent123
        DeliveryAgentEntity agent1 = deliveryAgentRepository.findByEmail("agent1@edots.dev").orElseGet(() -> {
            DeliveryAgentEntity a = new DeliveryAgentEntity();
            a.setEmail("agent1@edots.dev");
            a.setName("Ramesh Kumar");
            a.setPhone("+919876543210");
            a.setActive(true);
            return a;
        });
        agent1.setPasswordHash(passwordEncoder.encode("agent123"));
        deliveryAgentRepository.save(agent1);

        DeliveryAgentEntity agent2 = deliveryAgentRepository.findByEmail("agent2@edots.dev").orElseGet(() -> {
            DeliveryAgentEntity a = new DeliveryAgentEntity();
            a.setEmail("agent2@edots.dev");
            a.setName("Suresh Patel");
            a.setPhone("+919876543211");
            a.setActive(true);
            return a;
        });
        agent2.setPasswordHash(passwordEncoder.encode("agent123"));
        deliveryAgentRepository.save(agent2);

        // 3. Carrier Config: FastShip
        if (carrierConfigRepository.findByCarrierId("fastship").isEmpty()) {
            CarrierConfigEntity carrier = new CarrierConfigEntity();
            carrier.setCarrierId("fastship");
            carrier.setCarrierName("FastShip Logistics");
            carrier.setApiKeyHash(sha256("fastship-test-api-key-2026"));
            carrier.setStageMapping("{\"PICKED_UP\":\"PICKING\",\"IN_HUB\":\"PACKED\",\"IN_TRANSIT\":\"DISPATCHED\",\"OUT_FOR_DELIVERY\":\"OUT_FOR_DELIVERY\",\"DELIVERED\":\"DELIVERED\",\"FAILED_DELIVERY\":\"FAILED_ATTEMPT\",\"RETURNED_TO_ORIGIN\":\"RETURNED\"}");
            carrier.setActive(true);
            carrierConfigRepository.save(carrier);
        }

        // 4. Admin Config
        if (adminConfigRepository.findByConfigKey("max_delivery_attempts").isEmpty()) {
            AdminConfigEntity maxAttempts = new AdminConfigEntity();
            maxAttempts.setConfigKey("max_delivery_attempts");
            maxAttempts.setConfigValue("3");
            maxAttempts.setUpdatedBy(admin);
            adminConfigRepository.save(maxAttempts);
        }

        if (adminConfigRepository.findByConfigKey("stale_threshold_hours").isEmpty()) {
            AdminConfigEntity staleHours = new AdminConfigEntity();
            staleHours.setConfigKey("stale_threshold_hours");
            staleHours.setConfigValue("48");
            staleHours.setUpdatedBy(admin);
            adminConfigRepository.save(staleHours);
        }

        // 5. Notification Rules for all 8 stages
        for (Stage stage : Stage.values()) {
            if (notificationRuleRepository.findByEventTypeAndActiveTrue(stage.name()).isEmpty()) {
                NotificationRuleEntity rule = new NotificationRuleEntity();
                rule.setEventType(stage.name());
                rule.setChannels("[\"EMAIL\", \"IN_APP\"]");
                rule.setTemplateBody("Status update: Your order {{orderReference}} is now " + stage.getDisplayName() + ".");
                rule.setActive(true);
                rule.setCreatedBy(admin);
                notificationRuleRepository.save(rule);
            }
        }

        // 6. Sample Orders if empty or missing ORD-EDOTS-1003
        if (orderRepository.findByOrderReference("ORD-EDOTS-1003").isEmpty()) {
            createSampleOrder("ORD-EDOTS-1001", "Aarav Sharma", "aarav@example.com", "+919111111111", Stage.ORDER_PLACED, null, 0, 3, 2);
            createSampleOrder("ORD-EDOTS-1002", "Priya Verma", "priya@example.com", "+919222222222", Stage.DISPATCHED, agent1, 0, 3, 4);
            createSampleOrder("ORD-EDOTS-1003", "Rohan Mehta", "rohan@example.com", "+919333333333", Stage.OUT_FOR_DELIVERY, agent1, 0, 3, 6);
            createSampleOrder("ORD-EDOTS-1004", "Ananya Gupta", "ananya@example.com", "+919444444444", Stage.FAILED_ATTEMPT, agent1, 1, 3, 8);
            createSampleOrder("ORD-EDOTS-1005", "Vikram Singh", "vikram@example.com", "+919555555555", Stage.DELIVERED, agent1, 0, 3, 10);
        }

        if (orderRepository.count() <= 5) {
            createSampleOrder("ORD-2026-0001", "Aarav Sharma", "aarav@example.com", "+919111111111", Stage.ORDER_PLACED, null, 0, 3, 2);
            createSampleOrder("ORD-2026-0002", "Priya Verma", "priya@example.com", "+919222222222", Stage.PICKING, null, 0, 3, 4);
            createSampleOrder("ORD-2026-0003", "Rohan Mehta", "rohan@example.com", "+919333333333", Stage.PACKED, null, 0, 3, 6);
            createSampleOrder("ORD-2026-0004", "Ananya Gupta", "ananya@example.com", "+919444444444", Stage.DISPATCHED, agent1, 0, 3, 8);
            createSampleOrder("ORD-2026-0005", "Vikram Singh", "vikram@example.com", "+919555555555", Stage.OUT_FOR_DELIVERY, agent1, 0, 3, 10);
            createSampleOrder("ORD-2026-0006", "Neha Kapoor", "neha@example.com", "+919666666666", Stage.DELIVERED, agent1, 0, 3, 24);
            createSampleOrder("ORD-2026-0007", "Karan Johar", "karan@example.com", "+919777777777", Stage.FAILED_ATTEMPT, agent2, 1, 3, 18);
            createSampleOrder("ORD-2026-0008", "Simran Kaur", "simran@example.com", "+919888888888", Stage.RETURNED, agent2, 3, 3, 48);
            createSampleOrder("ORD-2026-0009", "Aditya Roy", "aditya@example.com", "+919999999999", Stage.DISPATCHED, agent2, 0, 3, 52); // STALE (>48h)
        }

        log.info("EDOTS seed data initialization complete.");
    }

    private void createSampleOrder(
        String ref, String name, String email, String phone,
        Stage stage, DeliveryAgentEntity agent, int attemptCount, int maxAttempts, int hoursAgo
    ) {
        OrderEntity order = new OrderEntity();
        order.setOrderReference(ref);
        order.setCustomerName(name);
        order.setCustomerEmail(email);
        order.setCustomerPhone(phone);
        order.setCurrentStage(stage);
        order.setAssignedAgent(agent);
        order.setAttemptCount(attemptCount);
        order.setMaxAttempts(maxAttempts);
        order.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        order.setUpdatedAt(LocalDateTime.now().minusHours(hoursAgo));
        orderRepository.save(order);

        OrderEventEntity event = new OrderEventEntity();
        event.setOrder(order);
        event.setEventType(stage.name());
        event.setNewStage(stage);
        event.setActorId("SYSTEM");
        event.setActorType(com.meetpgandhi.edots.domain.model.ActorType.SYSTEM);
        event.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        orderEventRepository.save(event);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
