package com.stillfresh.app.orderservice.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.stillfresh.app.orderservice.service.OrderService;

/**
 * Scheduled jobs to mark expired orders (pickup window passed) and send pickup reminders.
 */
@Component
public class OrderExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OrderExpiryScheduler.class);

    private final OrderService orderService;

    public OrderExpiryScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Run every 10 minutes: mark orders past pickup deadline as EXPIRED and send reminders. */
    @Scheduled(fixedDelayString = "${order.expiry.job.interval-ms:600000}")
    public void runExpiryAndReminders() {
        try {
            logger.debug("Running order expiry and pickup reminder job");
            orderService.processExpiredOrders();
            orderService.processPickupReminders();
        } catch (Exception e) {
            logger.warn("Order expiry/reminder job failed (will retry next run): {}", e.getMessage());
        }
    }
}
