package com.meetpgandhi.edots.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    public record LoginResponse(
        String accessToken,
        String role,
        Long userId,
        String email,
        String name
    ) {}

    public record TokenRefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
    ) {}
}
