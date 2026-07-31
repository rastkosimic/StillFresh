package com.stillfresh.app.userservice.listener;

import com.stillfresh.app.sharedentities.order.events.FraudFlagEvent;
import com.stillfresh.app.sharedentities.order.events.OrderNoShowEvent;
import com.stillfresh.app.userservice.service.StrikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Increments the customer's anti-abuse strike counters in response to order-service events:
 * bypass fraud flags (cancel at pickup location) and no-shows (expired unpicked orders).
 */
@Component
public class OrderStrikeListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderStrikeListener.class);

    @Autowired
    private StrikeService strikeService;

    @KafkaListener(topics = "${order.topic.fraud-flag:fraud-flag}", groupId = "user-service-group")
    public void handleFraudFlag(FraudFlagEvent event) {
        logger.info("Received FraudFlagEvent for user {}: {}", event.getUserId(), event.getReason());
        if (event.getUserId() == null) {
            return;
        }
        try {
            strikeService.recordBypassStrike(event.getUserId(), event.getReason());
        } catch (Exception e) {
            logger.error("Failed to record bypass strike for user {}", event.getUserId(), e);
        }
    }

    @KafkaListener(topics = "${order.topic.order-no-show:order-no-show}", groupId = "user-service-group")
    public void handleNoShow(OrderNoShowEvent event) {
        logger.info("Received OrderNoShowEvent for user {} (order {})", event.getUserId(), event.getOrderId());
        if (event.getUserId() == null) {
            return;
        }
        try {
            strikeService.recordNoShowStrike(event.getUserId());
        } catch (Exception e) {
            logger.error("Failed to record no-show strike for user {}", event.getUserId(), e);
        }
    }
}
