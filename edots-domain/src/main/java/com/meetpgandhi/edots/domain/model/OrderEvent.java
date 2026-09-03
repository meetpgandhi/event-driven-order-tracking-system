package com.meetpgandhi.edots.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

public record OrderEvent(
    String eventId,
    Long orderId,
    String orderReference,
    String eventType,
    Stage previousStage,
    Stage newStage,
    Actor actor,
    String reasonCode,
    Map<String, Object> metadata,
    Instant timestamp
) implements Serializable {}
