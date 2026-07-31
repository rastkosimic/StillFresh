package com.stillfresh.app.notificationservice.consumer;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.order.events.OrderPickupReminderEvent;

@Component
public class OrderPickupReminderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderPickupReminderConsumer.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${order.topic.order-pickup-reminder:order-pickup-reminder}", groupId = "notification-service")
    public void handleOrderPickupReminderEvent(OrderPickupReminderEvent event) {
        try {
            logger.info("Received pickup reminder event for order: {}, user: {}", event.getOrderId(), event.getUserId());

            String timeStr = event.getPickupBy() != null
                ? event.getPickupBy().format(TIME_FORMAT)
                : "the stated time";

            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.ORDER_PICKUP_REMINDER.name());
            data.put("orderId", event.getOrderId());
            data.put("offerId", String.valueOf(event.getOfferId()));
            if (event.getPickupBy() != null) data.put("pickupBy", event.getPickupBy().toString());
            if (event.getLocationName() != null) data.put("location_name", event.getLocationName());
            if (event.getChainName() != null) data.put("chain_name", event.getChainName());
            if (event.getWebsite() != null) data.put("website", event.getWebsite());
            if (event.getVendorImageUrl() != null) data.put("vendor_image_url", event.getVendorImageUrl());
            if (event.getAddress() != null) data.put("address", event.getAddress());
            if (event.getZipCode() != null) data.put("zip_code", event.getZipCode());
            if (event.getName() != null) data.put("name", event.getName());
            if (event.getImageUrl() != null) data.put("image_url", event.getImageUrl());

            NotificationRequestEvent notification = new NotificationRequestEvent(
                event.getUserId(),
                NotificationType.ORDER_PICKUP_REMINDER,
                "Pick up your order soon",
                "Your order must be picked up by " + timeStr + ".",
                data
            );
            notificationService.handleNotificationRequest(notification);
        } catch (Exception e) {
            logger.error("Failed to process order pickup reminder event", e);
        }
    }
}
