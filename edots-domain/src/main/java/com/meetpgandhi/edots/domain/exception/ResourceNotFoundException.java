package com.meetpgandhi.edots.domain.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException order(String reference) {
        return new ResourceNotFoundException("Order not found with reference: " + reference);
    }

    public static ResourceNotFoundException order(Long id) {
        return new ResourceNotFoundException("Order not found with id: " + id);
    }

    public static ResourceNotFoundException agent(String email) {
        return new ResourceNotFoundException("Delivery agent not found with email: " + email);
    }

    public static ResourceNotFoundException rule(Long id) {
        return new ResourceNotFoundException("Notification rule not found with id: " + id);
    }
}
