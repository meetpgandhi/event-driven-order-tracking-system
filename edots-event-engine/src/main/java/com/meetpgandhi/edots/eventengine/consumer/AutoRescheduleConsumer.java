package com.meetpgandhi.edots.eventengine.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetpgandhi.edots.domain.entity.AdminConfigEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OrderEventEntity;
import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.ActorType;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.AdminConfigRepository;
import com.meetpgandhi.edots.domain.repository.OrderEventRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.statemachine.OrderStateMachine;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import com.meetpgandhi.edots.eventengine.model.RescheduleTriggerEvent;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

@Component
public class AutoRescheduleConsumer {

    private static final Logger log = LoggerFactory.getLogger(AutoRescheduleConsumer.class);

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final AdminConfigRepository adminConfigRepository;
    private final OrderStateMachine stateMachine;
    private final OrderEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public AutoRescheduleConsumer(
        OrderRepository orderRepository,
        OrderEventRepository orderEventRepository,
        AdminConfigRepository adminConfigRepository,
        OrderStateMachine stateMachine,
        OrderEventProducer eventProducer,
        ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.adminConfigRepository = adminConfigRepository;
        this.stateMachine = stateMachine;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${edots.kafka.topics.reschedule-trigger:edots.reschedule.trigger}",
        groupId = "edots-reschedule-group",
        containerFactory = "kafkaListenerContainerFactory",
        autoStartup = "${edots.kafka.enabled:true}"
    )
    @Transactional
    public void consumeRescheduleTrigger(Object record) {
        try {
            RescheduleTriggerEvent event;
            if (record instanceof RescheduleTriggerEvent rte) {
                event = rte;
            } else {
                event = objectMapper.convertValue(record, RescheduleTriggerEvent.class);
            }

            processAutoReschedule(event);
        } catch (Exception ex) {
            log.error("Failed to execute auto-reschedule workflow: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void processAutoReschedule(RescheduleTriggerEvent event) {
        OrderEntity order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("Auto-reschedule aborted: order id {} not found", event.orderId());
            return;
        }

        // Only reschedule if currently in FAILED_ATTEMPT stage
        if (order.getCurrentStage() != Stage.FAILED_ATTEMPT) {
            log.info("Skipping auto-reschedule for order {}: current stage is {}",
                order.getOrderReference(), order.getCurrentStage());
            return;
        }

        int maxAttempts = resolveMaxAttempts(order);
        Actor systemActor = Actor.system();

        if (order.getAttemptCount() < maxAttempts) {
            // Attempt < max: Schedule next delivery slot and transition back to OUT_FOR_DELIVERY
            LocalDateTime nextSlot = calculateNextBusinessDaySlot();
            order.setNextDeliverySlot(nextSlot);
            order.setCurrentStage(Stage.OUT_FOR_DELIVERY);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            persistEvent(order, Stage.FAILED_ATTEMPT, Stage.OUT_FOR_DELIVERY, systemActor, "AUTO_RESCHEDULED");

            OrderEvent domainEvent = stateMachine.createTransitionEvent(
                order.getId(),
                order.getOrderReference(),
                Stage.FAILED_ATTEMPT,
                Stage.OUT_FOR_DELIVERY,
                systemActor,
                "AUTO_RESCHEDULED",
                Map.of("nextDeliverySlot", nextSlot.toString(), "attempt", order.getAttemptCount())
            );

            String agentName = order.getAssignedAgent() != null ? order.getAssignedAgent().getName() : "Unassigned";
            eventProducer.publishOrderEvent(
                domainEvent,
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                agentName,
                order.getAttemptCount(),
                maxAttempts
            );

            log.info("Auto-rescheduled order {} to next slot {} (Attempt {}/{})",
                order.getOrderReference(), nextSlot, order.getAttemptCount(), maxAttempts);

        } else {
            // Attempt >= max: Transition to RETURNED
            order.setCurrentStage(Stage.RETURNED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            persistEvent(order, Stage.FAILED_ATTEMPT, Stage.RETURNED, systemActor, "MAX_ATTEMPTS_EXCEEDED");

            OrderEvent domainEvent = stateMachine.createTransitionEvent(
                order.getId(),
                order.getOrderReference(),
                Stage.FAILED_ATTEMPT,
                Stage.RETURNED,
                systemActor,
                "MAX_ATTEMPTS_EXCEEDED",
                Map.of("totalAttempts", order.getAttemptCount())
            );

            eventProducer.publishOrderEvent(
                domainEvent,
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                "System Return",
                order.getAttemptCount(),
                maxAttempts
            );

            eventProducer.publishAuditEvent(AuditLogEvent.of(
                "ORDER_RETURNED_MAX_ATTEMPTS", "SYSTEM", ActorType.SYSTEM,
                order.getOrderReference(), "ORDER",
                String.format("Order returned to warehouse after %d failed attempts", order.getAttemptCount())
            ));

            log.info("Order {} reached max attempts ({}/{}). Transitioned to RETURNED.",
                order.getOrderReference(), order.getAttemptCount(), maxAttempts);
        }
    }

    private void persistEvent(OrderEntity order, Stage previous, Stage next, Actor actor, String reason) {
        OrderEventEntity eventEntity = new OrderEventEntity();
        eventEntity.setOrder(order);
        eventEntity.setEventType(next.name());
        eventEntity.setPreviousStage(previous);
        eventEntity.setNewStage(next);
        eventEntity.setActorId(actor.actorId());
        eventEntity.setActorType(actor.actorType());
        eventEntity.setReasonCode(reason);
        eventEntity.setCreatedAt(LocalDateTime.now());
        orderEventRepository.save(eventEntity);
    }

    public LocalDateTime calculateNextBusinessDaySlot() {
        LocalDate nextDay = LocalDate.now().plusDays(1);
        if (nextDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nextDay = nextDay.plusDays(1); // Monday
        }
        return LocalDateTime.of(nextDay, LocalTime.of(10, 0)); // 10:00 AM
    }

    private int resolveMaxAttempts(OrderEntity order) {
        Optional<AdminConfigEntity> configOpt = adminConfigRepository.findByConfigKey("max_delivery_attempts");
        if (configOpt.isPresent()) {
            try {
                return Integer.parseInt(configOpt.get().getConfigValue().trim());
            } catch (NumberFormatException ignored) {}
        }
        return order.getMaxAttempts() > 0 ? order.getMaxAttempts() : 3;
    }
}
