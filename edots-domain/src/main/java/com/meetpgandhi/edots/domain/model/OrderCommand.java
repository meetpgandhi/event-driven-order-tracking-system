package com.meetpgandhi.edots.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

public sealed interface OrderCommand permits 
    OrderCommand.TransitionStageCommand, 
    OrderCommand.RescheduleCommand, 
    OrderCommand.VerifyOtpCommand {

    record TransitionStageCommand(
        Long orderId,
        Stage newStage,
        Actor actor,
        String reasonCode,
        Map<String, Object> metadata
    ) implements OrderCommand {}

    record RescheduleCommand(
        Long orderId,
        LocalDateTime nextSlot,
        Actor actor
    ) implements OrderCommand {}

    record VerifyOtpCommand(
        Long orderId,
        String otpCode,
        Actor actor
    ) implements OrderCommand {}
}
