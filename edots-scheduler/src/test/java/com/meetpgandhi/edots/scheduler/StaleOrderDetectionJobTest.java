package com.meetpgandhi.edots.scheduler;

import com.meetpgandhi.edots.domain.entity.AdminConfigEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.AdminConfigRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaleOrderDetectionJobTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AdminConfigRepository adminConfigRepository;

    @Mock
    private OrderEventProducer eventProducer;

    private StaleOrderDetectionJob staleJob;

    @BeforeEach
    void setUp() {
        staleJob = new StaleOrderDetectionJob(orderRepository, adminConfigRepository, eventProducer);
        staleJob.setDefaultStaleHours(48);
    }

    @Test
    @DisplayName("Detect stale orders surfaces orders with no updates for 48+ hours and emits audit event")
    void shouldDetectAndLogStaleOrders() {
        AdminConfigEntity config = new AdminConfigEntity();
        config.setConfigKey("stale_threshold_hours");
        config.setConfigValue("48");
        when(adminConfigRepository.findByConfigKey("stale_threshold_hours")).thenReturn(Optional.of(config));

        OrderEntity staleOrder = new OrderEntity();
        staleOrder.setId(99L);
        staleOrder.setOrderReference("ORD-STALE-99");
        staleOrder.setCurrentStage(Stage.DISPATCHED);
        staleOrder.setUpdatedAt(LocalDateTime.now().minusHours(50));

        when(orderRepository.findStaleOrders(any(LocalDateTime.class), any())).thenReturn(List.of(staleOrder));

        List<OrderEntity> result = staleJob.detectStaleOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderReference()).isEqualTo("ORD-STALE-99");
        verify(eventProducer).publishAuditEvent(any());
    }
}
