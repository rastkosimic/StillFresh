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
import com.stillfresh.app.sharedentities.order.events.OrderPlacedEvent;

@Component
public class OrderPlacedConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderPlacedConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @KafkaListener(topics = "${order.topic.order-placed:order-placed}", groupId = "notification-service")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            logger.info("Received order placed event for user: {}, order: {}", event.getUserId(), event.getOrderId());
            
            // Create notification data
            Map<String, String> data = new HashMap<>();
            data.put("orderId", event.getOrderId());
            data.put("offerId", String.valueOf(event.getOfferId()));
            data.put("quantity", String.valueOf(event.getQuantity()));
            data.put("totalPrice", String.valueOf(event.getTotalPrice()));
            
            // Create and send notification
            NotificationRequestEvent notificationRequest = new NotificationRequestEvent(
                event.getUserId(),
                NotificationType.ORDER_CONFIRMED,
                "Order Confirmed",
                String.format("Your order for %d items has been confirmed. Total: $%.2f", 
                    event.getQuantity(), event.getTotalPrice()),
                data
            );
            
            notificationService.handleNotificationRequest(notificationRequest);
            
        } catch (Exception e) {
            logger.error("Failed to process order placed event", e);
        }
    }
} 