package com.meetpgandhi.edots.domain.exception;

import com.meetpgandhi.edots.domain.model.Stage;

public class InvalidStateTransitionException extends RuntimeException {
    private final String reasonCode;
    private final Stage currentStage;
    private final Stage attemptedStage;

    public InvalidStateTransitionException(String reasonCode, Stage currentStage, Stage attemptedStage, String message) {
        super(message);
        this.reasonCode = reasonCode;
        this.currentStage = currentStage;
        this.attemptedStage = attemptedStage;
    }

    public static InvalidStateTransitionException illegalTransition(Stage currentStage, Stage attemptedStage) {
        return new InvalidStateTransitionException(
            "ILLEGAL_STAGE_TRANSITION",
            currentStage,
            attemptedStage,
            String.format("Cannot transition order from stage %s to %s", currentStage, attemptedStage)
        );
    }

    public static InvalidStateTransitionException terminalStage(Stage currentStage, Stage attemptedStage) {
        return new InvalidStateTransitionException(
            "TERMINAL_STAGE_REACHED",
            currentStage,
            attemptedStage,
            String.format("Stage %s is terminal. No further transitions permitted.", currentStage)
        );
    }

    public static InvalidStateTransitionException missingReason(Stage currentStage, Stage attemptedStage) {
        return new InvalidStateTransitionException(
            "EXCEPTION_REASON_REQUIRED",
            currentStage,
            attemptedStage,
            "A valid reason code is required when reporting a FAILED_ATTEMPT."
        );
    }

    public static InvalidStateTransitionException otpRequired(Stage currentStage, Stage attemptedStage) {
        return new InvalidStateTransitionException(
            "OTP_VERIFICATION_REQUIRED",
            currentStage,
            attemptedStage,
            "Valid customer OTP verification is required before confirming delivery."
        );
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public Stage getAttemptedStage() {
        return attemptedStage;
    }
}
