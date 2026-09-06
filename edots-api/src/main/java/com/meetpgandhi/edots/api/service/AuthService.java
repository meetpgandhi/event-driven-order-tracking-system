package com.meetpgandhi.edots.api.service;

import com.meetpgandhi.edots.api.dto.AuthDtos.LoginRequest;
import com.meetpgandhi.edots.api.dto.AuthDtos.LoginResponse;
import com.meetpgandhi.edots.api.security.JwtTokenProvider;
import com.meetpgandhi.edots.domain.entity.AdminUserEntity;
import com.meetpgandhi.edots.domain.entity.DeliveryAgentEntity;
import com.meetpgandhi.edots.domain.repository.AdminUserRepository;
import com.meetpgandhi.edots.domain.repository.DeliveryAgentRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
        AdminUserRepository adminUserRepository,
        DeliveryAgentRepository deliveryAgentRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider tokenProvider
    ) {
        this.adminUserRepository = adminUserRepository;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        String password = request.password();

        // 1. Check if admin user
        Optional<AdminUserEntity> adminOpt = adminUserRepository.findByEmail(email);
        if (adminOpt.isPresent()) {
            AdminUserEntity admin = adminOpt.get();
            if (passwordEncoder.matches(password, admin.getPasswordHash())) {
                String token = tokenProvider.generateToken(admin.getEmail(), admin.getRole(), admin.getId(), admin.getName());
                return new LoginResponse(token, admin.getRole(), admin.getId(), admin.getEmail(), admin.getName());
            }
            throw new BadCredentialsException("Invalid password for administrator account");
        }

        // 2. Check if delivery agent
        Optional<DeliveryAgentEntity> agentOpt = deliveryAgentRepository.findByEmailAndActiveTrue(email);
        if (agentOpt.isPresent()) {
            DeliveryAgentEntity agent = agentOpt.get();
            if (passwordEncoder.matches(password, agent.getPasswordHash())) {
                String token = tokenProvider.generateToken(agent.getEmail(), "AGENT", agent.getId(), agent.getName());
                return new LoginResponse(token, "AGENT", agent.getId(), agent.getEmail(), agent.getName());
            }
            throw new BadCredentialsException("Invalid password for delivery agent account");
        }

        throw new BadCredentialsException("No active account found with email: " + email);
    }
}
