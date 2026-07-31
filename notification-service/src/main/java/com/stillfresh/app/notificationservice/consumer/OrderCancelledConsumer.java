package com.stillfresh.app.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.notificationservice.service.EmailService;
import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.notificationservice.service.UserContactService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.order.events.OrderCancelledEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderCancelledConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderCancelledConsumer.class);
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserContactService userContactService;
    
    @KafkaListener(topics = "${order.topic.order-cancelled:order-cancelled}", 
                   groupId = "notification-service")
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        try {
            logger.info("Received order cancelled event for order: {}, cancelled by: {}", 
                       event.getOrderId(), event.getCancelledBy());
            
            // Create notification data
            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.ORDER_CANCELLED.name());
            data.put("orderId", event.getOrderId());
            data.put("offerId", String.valueOf(event.getOfferId()));
            data.put("quantity", String.valueOf(event.getQuantity()));
            data.put("totalPrice", String.valueOf(event.getTotalPrice()));
            data.put("cancelledBy", event.getCancelledBy());
            if (event.getReason() != null) data.put("reason", event.getReason());
            if (event.getLocationName() != null) data.put("location_name", event.getLocationName());
            if (event.getChainName() != null) data.put("chain_name", event.getChainName());
            if (event.getWebsite() != null) data.put("website", event.getWebsite());
            if (event.getVendorImageUrl() != null) data.put("vendor_image_url", event.getVendorImageUrl());
            if (event.getAddress() != null) data.put("address", event.getAddress());
            if (event.getZipCode() != null) data.put("zip_code", event.getZipCode());
            if (event.getName() != null) data.put("name", event.getName());
            if (event.getImageUrl() != null) data.put("image_url", event.getImageUrl());
            
            // Notify the appropriate party based on who cancelled
            if ("CUSTOMER".equals(event.getCancelledBy())) {
                // Customer cancelled - notify vendor
                NotificationRequestEvent vendorNotification = new NotificationRequestEvent(
                    event.getVendorId().toString(), // Vendor ID
                    NotificationType.ORDER_CANCELLED,
                    "Order Cancelled by Customer",
                    String.format("Order #%s has been cancelled by the customer. Quantity: %d, Total: $%.2f", 
                        event.getOrderId(), event.getQuantity(), event.getTotalPrice()),
                    data
                );
                notificationService.handleNotificationRequest(vendorNotification);
                logger.info("Sent cancellation notification to vendor: {}", event.getVendorId());
                
            } else if ("VENDOR".equals(event.getCancelledBy())) {
                // Vendor rejected - notify customer
                NotificationRequestEvent customerNotification = new NotificationRequestEvent(
                    event.getUserId(), // Customer ID
                    NotificationType.ORDER_CANCELLED,
                    "Order Rejected by Vendor",
                    String.format("Your order #%s has been rejected by the vendor. %s", 
                        event.getOrderId(),
                        event.getReason() != null ? "Reason: " + event.getReason() : ""),
                    data
                );
                notificationService.handleNotificationRequest(customerNotification);
                logger.info("Sent rejection notification to customer: {}", event.getUserId());

                // Email the customer with the reason and refund confirmation.
                sendCustomerCancellationEmail(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process order cancelled event", e);
        }
    }

    private void sendCustomerCancellationEmail(OrderCancelledEvent event) {
        if (!emailService.isEnabled()) {
            return;
        }
        String to = userContactService.resolveEmail(event.getUserId());
        if (to == null || to.isBlank()) {
            logger.warn("No email resolved for user {}; skipping cancellation email", event.getUserId());
            return;
        }
        String reasonLine = event.getReason() != null && !event.getReason().isBlank()
            ? String.format("Reason: %s%n%n", event.getReason())
            : "";
        String body = String.format(
            "Hello,%n%n" +
            "We're sorry - your order #%s has been rejected by the vendor.%n%n" +
            "%s" +
            "Any amount charged for this order will be refunded to your original payment method.%n%n" +
            "Thank you for your understanding,%nThe StillFresh team",
            event.getOrderId(), reasonLine
        );
        emailService.sendEmail(to, "StillFresh - Order Cancelled", body);
    }
}

