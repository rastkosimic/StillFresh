package com.stillfresh.app.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.order.events.VendorOrderNotificationEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class VendorOrderNotificationConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(VendorOrderNotificationConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @KafkaListener(topics = "${order.topic.vendor-order-notification:vendor-order-notification}", 
                   groupId = "notification-service")
    public void handleVendorOrderNotificationEvent(VendorOrderNotificationEvent event) {
        try {
            logger.info("Received vendor order notification for vendor: {}, order: {}", 
                       event.getVendorId(), event.getOrderId());
            
            // Create notification data
            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.ORDER_RECEIVED.name());
            data.put("orderId", event.getOrderId());
            data.put("userId", event.getUserId());
            data.put("offerId", String.valueOf(event.getOfferId()));
            data.put("quantity", String.valueOf(event.getQuantity()));
            data.put("totalPrice", String.valueOf(event.getTotalPrice()));
            if (event.getOfferName() != null) data.put("offerName", event.getOfferName());
            if (event.getLocationName() != null) data.put("location_name", event.getLocationName());
            if (event.getChainName() != null) data.put("chain_name", event.getChainName());
            if (event.getWebsite() != null) data.put("website", event.getWebsite());
            if (event.getVendorImageUrl() != null) data.put("vendor_image_url", event.getVendorImageUrl());
            if (event.getAddress() != null) data.put("address", event.getAddress());
            if (event.getZipCode() != null) data.put("zip_code", event.getZipCode());
            if (event.getOfferName() != null) data.put("name", event.getOfferName());
            if (event.getImageUrl() != null) data.put("image_url", event.getImageUrl());
            
            // Create and send notification to vendor
            NotificationRequestEvent notificationRequest = new NotificationRequestEvent(
                event.getVendorId().toString(), // Vendor ID as user ID
                NotificationType.ORDER_RECEIVED,
                "New Order Received",
                String.format("You have received a new order for %s (Qty: %d) - Total: $%.2f", 
                    event.getOfferName(), event.getQuantity(), event.getTotalPrice()),
                data
            );
            
            notificationService.handleNotificationRequest(notificationRequest);
            
        } catch (Exception e) {
            logger.error("Failed to process vendor order notification event", e);
        }
    }
}
