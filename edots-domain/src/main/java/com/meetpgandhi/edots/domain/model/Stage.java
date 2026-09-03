package com.meetpgandhi.edots.domain.model;

public enum Stage {
    ORDER_PLACED("Order Placed", false),
    PICKING("Picking", false),
    PACKED("Packed", false),
    DISPATCHED("Dispatched", false),
    OUT_FOR_DELIVERY("Out for Delivery", false),
    DELIVERED("Delivered", true),
    FAILED_ATTEMPT("Failed Attempt", false),
    RETURNED("Returned", true);

    private final String displayName;
    private final boolean terminal;

    Stage(String displayName, boolean terminal) {
        this.displayName = displayName;
        this.terminal = terminal;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
