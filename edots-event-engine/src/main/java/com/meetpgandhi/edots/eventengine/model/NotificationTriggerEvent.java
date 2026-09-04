package com.meetpgandhi.edots.eventengine.model;

import com.meetpgandhi.edots.domain.model.Stage;
import java.io.Serializable;
import java.time.Instant;

public record NotificationTriggerEvent(
    Long orderId,
    String orderReference,
    Stage previousStage,
    Stage newStage,
    String customerEmail,
    String customerPhone,
    String agentName,
    String reasonCode,
    Instant timestamp
) implements Serializable {}
