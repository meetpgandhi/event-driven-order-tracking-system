package com.meetpgandhi.edots.eventengine.model;

import com.meetpgandhi.edots.domain.model.ActorType;
import java.io.Serializable;
import java.time.Instant;

public record AuditLogEvent(
    String eventType,
    String actorId,
    ActorType actorType,
    String referenceId,
    String referenceType,
    String detail,
    Instant timestamp
) implements Serializable {
    public static AuditLogEvent of(
        String eventType,
        String actorId,
        ActorType actorType,
        String referenceId,
        String referenceType,
        String detail
    ) {
        return new AuditLogEvent(eventType, actorId, actorType, referenceId, referenceType, detail, Instant.now());
    }
}
