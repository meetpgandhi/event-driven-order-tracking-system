package com.meetpgandhi.edots.domain.statemachine;

import com.meetpgandhi.edots.domain.exception.InvalidStateTransitionException;
import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class OrderStateMachine {

    private static final Map<Stage, Set<Stage>> ALLOWED_TRANSITIONS = Map.of(
        Stage.ORDER_PLACED, Set.of(Stage.PICKING),
        Stage.PICKING, Set.of(Stage.PACKED),
        Stage.PACKED, Set.of(Stage.DISPATCHED),
        Stage.DISPATCHED, Set.of(Stage.OUT_FOR_DELIVERY),
        Stage.OUT_FOR_DELIVERY, Set.of(Stage.DELIVERED, Stage.FAILED_ATTEMPT),
        Stage.FAILED_ATTEMPT, Set.of(Stage.OUT_FOR_DELIVERY, Stage.RETURNED),
        Stage.DELIVERED, Collections.emptySet(),
        Stage.RETURNED, Collections.emptySet()
    );

    public boolean canTransition(Stage currentStage, Stage targetStage) {
        if (currentStage == null || targetStage == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStage, Collections.emptySet()).contains(targetStage);
    }

    public Set<Stage> getNextValidStages(Stage currentStage) {
        if (currentStage == null) {
            return Collections.emptySet();
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStage, Collections.emptySet());
    }

    public void validateTransition(Stage currentStage, Stage targetStage, String reasonCode, boolean otpVerified) {
        if (currentStage == null) {
            throw new IllegalArgumentException("Current stage cannot be null");
        }
        if (targetStage == null) {
            throw new IllegalArgumentException("Target stage cannot be null");
        }

        if (currentStage.isTerminal()) {
            throw InvalidStateTransitionException.terminalStage(currentStage, targetStage);
        }

        if (!canTransition(currentStage, targetStage)) {
            throw InvalidStateTransitionException.illegalTransition(currentStage, targetStage);
        }

        if (targetStage == Stage.FAILED_ATTEMPT && (reasonCode == null || reasonCode.trim().isEmpty())) {
            throw InvalidStateTransitionException.missingReason(currentStage, targetStage);
        }

        if (targetStage == Stage.DELIVERED && !otpVerified) {
            throw InvalidStateTransitionException.otpRequired(currentStage, targetStage);
        }
    }

    public OrderEvent createTransitionEvent(
        Long orderId,
        String orderReference,
        Stage previousStage,
        Stage newStage,
        Actor actor,
        String reasonCode,
        Map<String, Object> metadata
    ) {
        return new OrderEvent(
            UUID.randomUUID().toString(),
            orderId,
            orderReference,
            newStage.name(),
            previousStage,
            newStage,
            actor,
            reasonCode,
            metadata != null ? metadata : Collections.emptyMap(),
            Instant.now()
        );
    }
}
