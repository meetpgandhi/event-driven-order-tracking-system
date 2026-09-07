package com.meetpgandhi.edots.api.service;

import com.meetpgandhi.edots.api.dto.AgentDtos.OtpGenerateResponse;
import com.meetpgandhi.edots.api.dto.AgentDtos.OtpVerifyResponse;
import com.meetpgandhi.edots.domain.entity.DeliveryAgentEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.entity.OtpTokenEntity;
import com.meetpgandhi.edots.domain.exception.OtpVerificationException;
import com.meetpgandhi.edots.domain.repository.DeliveryAgentRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.domain.repository.OtpTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    private OtpService otpService;

    @Captor
    private ArgumentCaptor<OtpTokenEntity> tokenCaptor;

    private OrderEntity sampleOrder;
    private DeliveryAgentEntity sampleAgent;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpTokenRepository, orderRepository, deliveryAgentRepository);

        sampleOrder = new OrderEntity();
        sampleOrder.setId(10L);
        sampleOrder.setOrderReference("ORD-TEST-001");

        sampleAgent = new DeliveryAgentEntity();
        sampleAgent.setId(1L);
        sampleAgent.setEmail("agent1@edots.dev");
    }

    @Test
    @DisplayName("Generate OTP creates 6-digit token with 10-minute expiry")
    void shouldGenerateValidOtpToken() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));
        when(deliveryAgentRepository.findByEmail("agent1@edots.dev")).thenReturn(Optional.of(sampleAgent));

        OtpGenerateResponse response = otpService.generateOtp(10L, "agent1@edots.dev");

        assertThat(response.orderId()).isEqualTo(10L);
        assertThat(response.debugOtp()).isNotNull().hasSize(6);
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now());

        verify(otpTokenRepository).save(tokenCaptor.capture());
        OtpTokenEntity saved = tokenCaptor.getValue();
        assertThat(saved.getOtpCode()).isEqualTo(response.debugOtp());
        assertThat(saved.isVerified()).isFalse();
    }

    @Test
    @DisplayName("Verify OTP with correct code succeeds and sets verified=true")
    void shouldVerifyCorrectOtp() {
        OtpTokenEntity token = new OtpTokenEntity();
        token.setOrder(sampleOrder);
        token.setAgent(sampleAgent);
        token.setOtpCode("654321");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setVerified(false);

        when(otpTokenRepository.findTopByOrderIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(token));

        OtpVerifyResponse response = otpService.verifyOtp(10L, "654321");

        assertThat(response.verified()).isTrue();
        assertThat(token.isVerified()).isTrue();
        verify(otpTokenRepository).save(token);
    }

    @Test
    @DisplayName("Verify OTP with wrong code throws OtpVerificationException")
    void shouldThrowWhenOtpIsInvalid() {
        OtpTokenEntity token = new OtpTokenEntity();
        token.setOrder(sampleOrder);
        token.setOtpCode("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setVerified(false);

        when(otpTokenRepository.findTopByOrderIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp(10L, "999999"))
            .isInstanceOf(OtpVerificationException.class)
            .hasFieldOrPropertyWithValue("reasonCode", "OTP_INVALID");
    }

    @Test
    @DisplayName("Verify OTP when expired throws OtpVerificationException")
    void shouldThrowWhenOtpIsExpired() {
        OtpTokenEntity token = new OtpTokenEntity();
        token.setOrder(sampleOrder);
        token.setOtpCode("123456");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired
        token.setVerified(false);

        when(otpTokenRepository.findTopByOrderIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp(10L, "123456"))
            .isInstanceOf(OtpVerificationException.class)
            .hasFieldOrPropertyWithValue("reasonCode", "OTP_EXPIRED");
    }
}
