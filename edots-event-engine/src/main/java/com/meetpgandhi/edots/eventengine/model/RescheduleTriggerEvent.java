package com.meetpgandhi.edots.eventengine.model;

import java.io.Serializable;
import java.time.Instant;

public record RescheduleTriggerEvent(
    Long orderId,
    String orderReference,
    int currentAttemptCount,
    int maxAttempts,
    String reasonCode,
    Instant timestamp
) implements Serializable {}
