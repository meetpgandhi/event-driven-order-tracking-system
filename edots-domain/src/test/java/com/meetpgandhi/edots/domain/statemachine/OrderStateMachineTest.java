package com.meetpgandhi.edots.domain.statemachine;

import com.meetpgandhi.edots.domain.exception.InvalidStateTransitionException;
import com.meetpgandhi.edots.domain.model.Actor;
import com.meetpgandhi.edots.domain.model.OrderEvent;
import com.meetpgandhi.edots.domain.model.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Nested
    @DisplayName("Valid Stage Transitions")
    class ValidTransitions {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
            "ORDER_PLACED, PICKING",
            "PICKING, PACKED",
            "PACKED, DISPATCHED",
            "DISPATCHED, OUT_FOR_DELIVERY",
            "OUT_FOR_DELIVERY, DELIVERED",
            "OUT_FOR_DELIVERY, FAILED_ATTEMPT",
            "FAILED_ATTEMPT, OUT_FOR_DELIVERY",
            "FAILED_ATTEMPT, RETURNED"
        })
        void shouldAllowValidTransition(Stage from, Stage to) {
            assertThat(stateMachine.canTransition(from, to)).isTrue();
        }

        @Test
        void shouldValidateOutForDeliveryToDeliveredWhenOtpVerified() {
            assertThatCode(() -> stateMachine.validateTransition(
                Stage.OUT_FOR_DELIVERY, Stage.DELIVERED, null, true
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldValidateOutForDeliveryToFailedAttemptWhenReasonProvided() {
            assertThatCode(() -> stateMachine.validateTransition(
                Stage.OUT_FOR_DELIVERY, Stage.FAILED_ATTEMPT, "RECIPIENT_ABSENT", false
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldValidateFailedAttemptToOutForDelivery() {
            assertThatCode(() -> stateMachine.validateTransition(
                Stage.FAILED_ATTEMPT, Stage.OUT_FOR_DELIVERY, null, false
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldValidateFailedAttemptToReturned() {
            assertThatCode(() -> stateMachine.validateTransition(
                Stage.FAILED_ATTEMPT, Stage.RETURNED, null, false
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldReturnNextValidStages() {
            Set<Stage> nextStages = stateMachine.getNextValidStages(Stage.OUT_FOR_DELIVERY);
            assertThat(nextStages).containsExactlyInAnyOrder(Stage.DELIVERED, Stage.FAILED_ATTEMPT);
        }

        @Test
        void shouldCreateTransitionEventProperly() {
            Actor actor = Actor.agent("agent1@edots.dev");
            OrderEvent event = stateMachine.createTransitionEvent(
                100L, "ORD-1234", Stage.DISPATCHED, Stage.OUT_FOR_DELIVERY, actor, null, Map.of("lat", 28.5)
            );

            assertThat(event).isNotNull();
            assertThat(event.orderId()).isEqualTo(100L);
            assertThat(event.orderReference()).isEqualTo("ORD-1234");
            assertThat(event.previousStage()).isEqualTo(Stage.DISPATCHED);
            assertThat(event.newStage()).isEqualTo(Stage.OUT_FOR_DELIVERY);
            assertThat(event.actor()).isEqualTo(actor);
            assertThat(event.timestamp()).isNotNull();
            assertThat(event.metadata()).containsEntry("lat", 28.5);
        }
    }

    @Nested
    @DisplayName("Invalid Stage Transitions & Rejections")
    class InvalidTransitions {

        @ParameterizedTest(name = "Disallow {0} -> {1}")
        @CsvSource({
            "ORDER_PLACED, DELIVERED",
            "ORDER_PLACED, PACKED",
            "ORDER_PLACED, DISPATCHED",
            "ORDER_PLACED, OUT_FOR_DELIVERY",
            "PICKING, DISPATCHED",
            "PICKING, DELIVERED",
            "PACKED, OUT_FOR_DELIVERY",
            "PACKED, DELIVERED",
            "DISPATCHED, DELIVERED",
            "OUT_FOR_DELIVERY, PICKING",
            "OUT_FOR_DELIVERY, PACKED",
            "FAILED_ATTEMPT, PICKING",
            "FAILED_ATTEMPT, DELIVERED"
        })
        void shouldRejectInvalidTransitions(Stage from, Stage to) {
            assertThat(stateMachine.canTransition(from, to)).isFalse();

            assertThatThrownBy(() -> stateMachine.validateTransition(from, to, null, true))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasFieldOrPropertyWithValue("reasonCode", "ILLEGAL_STAGE_TRANSITION");
        }

        @ParameterizedTest(name = "Terminal stage DELIVERED -> {0}")
        @EnumSource(Stage.class)
        void shouldRejectAnyTransitionFromDelivered(Stage targetStage) {
            assertThat(stateMachine.canTransition(Stage.DELIVERED, targetStage)).isFalse();

            assertThatThrownBy(() -> stateMachine.validateTransition(Stage.DELIVERED, targetStage, null, true))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasFieldOrPropertyWithValue("reasonCode", "TERMINAL_STAGE_REACHED");
        }

        @ParameterizedTest(name = "Terminal stage RETURNED -> {0}")
        @EnumSource(Stage.class)
        void shouldRejectAnyTransitionFromReturned(Stage targetStage) {
            assertThat(stateMachine.canTransition(Stage.RETURNED, targetStage)).isFalse();

            assertThatThrownBy(() -> stateMachine.validateTransition(Stage.RETURNED, targetStage, null, true))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasFieldOrPropertyWithValue("reasonCode", "TERMINAL_STAGE_REACHED");
        }

        @Test
        void shouldRejectDeliveredWithoutOtpVerification() {
            assertThatThrownBy(() -> stateMachine.validateTransition(
                Stage.OUT_FOR_DELIVERY, Stage.DELIVERED, null, false
            ))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasFieldOrPropertyWithValue("reasonCode", "OTP_VERIFICATION_REQUIRED");
        }

        @Test
        void shouldRejectFailedAttemptWithoutReasonCode() {
            assertThatThrownBy(() -> stateMachine.validateTransition(
                Stage.OUT_FOR_DELIVERY, Stage.FAILED_ATTEMPT, "", false
            ))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasFieldOrPropertyWithValue("reasonCode", "EXCEPTION_REASON_REQUIRED");

            assertThatThrownBy(() -> stateMachine.validateTransition(
                Stage.OUT_FOR_DELIVERY, Stage.FAILED_ATTEMPT, null, false
            ))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasFieldOrPropertyWithValue("reasonCode", "EXCEPTION_REASON_REQUIRED");
        }

        @Test
        void shouldThrowOnNullStages() {
            assertThatThrownBy(() -> stateMachine.validateTransition(null, Stage.PICKING, null, false))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> stateMachine.validateTransition(Stage.PICKING, null, null, false))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
