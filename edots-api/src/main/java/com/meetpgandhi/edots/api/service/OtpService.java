package com.meetpgandhi.edots.api.service;

import com.meetpgandhi.edots.api.dto.AgentDtos.OtpGenerateResponse;
import com.meetpgandhi.edots.api.dto.AgentDtos.OtpVerifyResponse;
import com.meetpgandhi.edots.domain.entity.DeliveryAgentEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OtpTokenEntity;
import com.meetpgandhi.edots.domain.exception.OtpVerificationException;
import com.meetpgandhi.edots.domain.exception.ResourceNotFoundException;
import com.meetpgandhi.edots.domain.repository.DeliveryAgentRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.repository.OtpTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final OrderRepository orderRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
        OtpTokenRepository otpTokenRepository,
        OrderRepository orderRepository,
        DeliveryAgentRepository deliveryAgentRepository
    ) {
        this.otpTokenRepository = otpTokenRepository;
        this.orderRepository = orderRepository;
        this.deliveryAgentRepository = deliveryAgentRepository;
    }

    @Transactional
    public OtpGenerateResponse generateOtp(Long orderId, String agentEmail) {
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> ResourceNotFoundException.order(orderId));

        DeliveryAgentEntity agent = deliveryAgentRepository.findByEmail(agentEmail)
            .orElseThrow(() -> ResourceNotFoundException.agent(agentEmail));

        // Generate 6 digit code (100000 - 999999)
        int code = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(code);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        OtpTokenEntity token = new OtpTokenEntity();
        token.setOrder(order);
        token.setAgent(agent);
        token.setOtpCode(otpCode);
        token.setExpiresAt(expiresAt);
        token.setVerified(false);

        otpTokenRepository.save(token);

        return new OtpGenerateResponse(
            orderId,
            "OTP generated successfully. Valid for 10 minutes.",
            expiresAt,
            otpCode
        );
    }

    @Transactional
    public OtpVerifyResponse verifyOtp(Long orderId, String otpCode) {
        OtpTokenEntity token = otpTokenRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
            .orElseThrow(OtpVerificationException::notFound);

        if (token.isVerified()) {
            return new OtpVerifyResponse(orderId, true, "OTP was already verified.");
        }

        if (token.isExpired()) {
            throw OtpVerificationException.expired();
        }

        if (!token.getOtpCode().equals(otpCode.trim())) {
            throw OtpVerificationException.invalid();
        }

        token.setVerified(true);
        otpTokenRepository.save(token);

        return new OtpVerifyResponse(orderId, true, "OTP verified successfully. Delivery can be completed.");
    }

    @Transactional(readOnly = true)
    public boolean isOtpVerified(Long orderId) {
        return otpTokenRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
            .map(OtpTokenEntity::isVerified)
            .orElse(false);
    }
}
