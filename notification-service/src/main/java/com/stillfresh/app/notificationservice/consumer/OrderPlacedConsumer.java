package com.stillfresh.app.notificationservice.consumer;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.notificationservice.service.EmailService;
import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.order.events.OrderPlacedEvent;

@Component
public class OrderPlacedConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderPlacedConsumer.class);
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;
    
    @KafkaListener(topics = "${order.topic.order-placed:order-placed}", groupId = "notification-service")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            logger.info("Received order placed event for user: {}, order: {}", event.getUserId(), event.getOrderId());
            
            // Create notification data
            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.ORDER_CONFIRMED.name());
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

            // Email receipt is the primary record (disabled unless notification.email.enabled=true).
            // OrderPlacedEvent already carries the customer email, so no lookup is needed here.
            if (emailService.isEnabled() && event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank()) {
                emailService.sendEmail(event.getCustomerEmail(), "StillFresh - Order Confirmation", buildReceipt(event));
            }

        } catch (Exception e) {
            logger.error("Failed to process order placed event", e);
        }
    }

    private String buildReceipt(OrderPlacedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Hello,%n%nYour order has been confirmed. Here are the details:%n%n"));
        sb.append(String.format("Order ID:   %s%n", event.getOrderId()));
        if (event.getName() != null) {
            sb.append(String.format("Item:       %s%n", event.getName()));
        }
        sb.append(String.format("Quantity:   %d%n", event.getQuantity()));
        sb.append(String.format("Total:      $%.2f%n", event.getTotalPrice()));

        String vendor = event.getLocationName() != null ? event.getLocationName() : event.getChainName();
        if (vendor != null) {
            sb.append(String.format("Vendor:     %s%n", vendor));
        }
        if (event.getAddress() != null) {
            String address = event.getAddress();
            if (event.getZipCode() != null) {
                address = address + ", " + event.getZipCode();
            }
            sb.append(String.format("Pickup at:  %s%n", address));
        }
        sb.append(String.format("%nPlease pick up your order within the offer's pickup window.%n"));
        sb.append(String.format("Payment is handled exclusively through the StillFresh app.%n%n"));
        sb.append(String.format("Thank you for helping reduce food waste!%nThe StillFresh team"));
        return sb.toString();
    }
} 