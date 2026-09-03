package com.meetpgandhi.edots.domain.exception;

public class OtpVerificationException extends RuntimeException {
    private final String reasonCode;

    public OtpVerificationException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public static OtpVerificationException expired() {
        return new OtpVerificationException("OTP_EXPIRED", "The provided OTP has expired. Please request a new OTP.");
    }

    public static OtpVerificationException invalid() {
        return new OtpVerificationException("OTP_INVALID", "The provided OTP is invalid.");
    }

    public static OtpVerificationException notFound() {
        return new OtpVerificationException("OTP_NOT_FOUND", "No active OTP token found for this order. Please generate one first.");
    }

    public static OtpVerificationException alreadyVerified() {
        return new OtpVerificationException("OTP_ALREADY_VERIFIED", "This OTP token has already been verified.");
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
