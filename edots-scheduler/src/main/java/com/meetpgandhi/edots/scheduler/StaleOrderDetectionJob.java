package com.meetpgandhi.edots.scheduler;

import com.meetpgandhi.edots.domain.entity.AdminConfigEntity;
import com.meetpgandhi.edots.domain.entity.OrderEntity;
import com.meetpgandhi.edots.domain.model.ActorType;
import com.meetpgandhi.edots.domain.model.Stage;
import com.meetpgandhi.edots.domain.repository.AdminConfigRepository;
import com.meetpgandhi.edots.domain.repository.OrderRepository;
import com.meetpgandhi.edots.eventengine.model.AuditLogEvent;
import com.meetpgandhi.edots.eventengine.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StaleOrderDetectionJob {

    private static final Logger log = LoggerFactory.getLogger(StaleOrderDetectionJob.class);

    private static final List<Stage> TERMINAL_STAGES = List.of(Stage.DELIVERED, Stage.RETURNED);

    private final OrderRepository orderRepository;
    private final AdminConfigRepository adminConfigRepository;
    private final OrderEventProducer eventProducer;

    @Value("${edots.scheduling.stale-threshold-hours:48}")
    private int defaultStaleHours = 48;

    public StaleOrderDetectionJob(
        OrderRepository orderRepository,
        AdminConfigRepository adminConfigRepository,
        OrderEventProducer eventProducer
    ) {
        this.orderRepository = orderRepository;
        this.adminConfigRepository = adminConfigRepository;
        this.eventProducer = eventProducer;
    }

    public void setDefaultStaleHours(int defaultStaleHours) {
        this.defaultStaleHours = defaultStaleHours;
    }

    @Scheduled(cron = "${edots.scheduling.stale-cron:0 0 * * * ?}")
    public List<OrderEntity> detectStaleOrders() {
        int thresholdHours = resolveStaleThresholdHours();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(thresholdHours);

        log.info("Running StaleOrderDetectionJob: finding active orders inactive since {} (threshold: {}h)",
            cutoff, thresholdHours);

        List<OrderEntity> staleOrders = orderRepository.findStaleOrders(cutoff, TERMINAL_STAGES);

        if (!staleOrders.isEmpty()) {
            log.warn("SURFACED {} STALE ORDERS with no activity for {} hours!", staleOrders.size(), thresholdHours);

            for (OrderEntity order : staleOrders) {
                eventProducer.publishAuditEvent(AuditLogEvent.of(
                    "STALE_ORDER_DETECTED", "SYSTEM", ActorType.SYSTEM,
                    order.getOrderReference(), "ORDER",
                    String.format("Order inactive for > %d hours in stage %s", thresholdHours, order.getCurrentStage())
                ));
            }
        } else {
            log.info("No stale orders detected.");
        }

        return staleOrders;
    }

    private int resolveStaleThresholdHours() {
        return adminConfigRepository.findByConfigKey("stale_threshold_hours")
            .map(AdminConfigEntity::getConfigValue)
            .map(val -> {
                try {
                    return Integer.parseInt(val.trim());
                } catch (NumberFormatException e) {
                    return defaultStaleHours;
                }
            })
            .orElse(defaultStaleHours);
    }
}
