package com.meetpgandhi.edots.domain.model;

import java.io.Serializable;

public record Actor(String actorId, ActorType actorType) implements Serializable {
    public static Actor system() {
        return new Actor("SYSTEM", ActorType.SYSTEM);
    }

    public static Actor agent(String agentEmail) {
        return new Actor(agentEmail, ActorType.AGENT);
    }

    public static Actor admin(String adminEmail) {
        return new Actor(adminEmail, ActorType.ADMIN);
    }

    public static Actor carrier(String carrierId) {
        return new Actor(carrierId, ActorType.CARRIER);
    }
}
