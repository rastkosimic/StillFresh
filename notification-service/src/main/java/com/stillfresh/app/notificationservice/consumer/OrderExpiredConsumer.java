package com.stillfresh.app.notificationservice.consumer;

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
import com.stillfresh.app.sharedentities.order.events.OrderExpiredEvent;

@Component
public class OrderExpiredConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderExpiredConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${order.topic.order-expired:order-expired}", groupId = "notification-service")
    public void handleOrderExpiredEvent(OrderExpiredEvent event) {
        try {
            logger.info("Received order expired event for order: {}, user: {}", event.getOrderId(), event.getUserId());

            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.ORDER_EXPIRED.name());
            data.put("orderId", event.getOrderId());
            data.put("offerId", String.valueOf(event.getOfferId()));
            data.put("quantity", String.valueOf(event.getQuantity()));
            data.put("totalPrice", String.valueOf(event.getTotalPrice()));
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
                NotificationType.ORDER_EXPIRED,
                "Reservation Expired",
                "Your reservation has expired. You were not charged.",
                data
            );
            notificationService.handleNotificationRequest(notification);
        } catch (Exception e) {
            logger.error("Failed to process order expired event", e);
        }
    }
}
