package com.stillfresh.app.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.vendor.events.BankingModelChangedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer for BankingModelChangedEvent
 * Sends in-app/push notifications to all chain locations when banking model is switched
 */
@Component
public class BankingModelChangedConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BankingModelChangedConsumer.class);
    
    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${vendor.topic.banking-model-changed:banking-model-changed}", 
                   groupId = "notification-service")
    public void handleBankingModelChangedEvent(BankingModelChangedEvent event) {
        try {
            logger.info("Received BankingModelChangedEvent: chainId={}, chainName={}, newModel={}, previousModel={}", 
                     event.getChainId(), event.getChainName(), event.getNewBankingModel(), event.getPreviousBankingModel());
            
            // Create notification data
            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.BANKING_MODEL_CHANGED.name());
            data.put("chainId", event.getChainId());
            data.put("chainName", event.getChainName());
            data.put("newBankingModel", event.getNewBankingModel());
            data.put("previousBankingModel", event.getPreviousBankingModel());
            data.put("changedByVendorId", String.valueOf(event.getChangedByVendorId()));
            data.put("changedByEmail", event.getChangedByEmail());
            if (event.getHeadquartersVendorId() != null) {
                data.put("headquartersVendorId", String.valueOf(event.getHeadquartersVendorId()));
                data.put("headquartersEmail", event.getHeadquartersEmail());
            }
            data.put("changedAt", event.getChangedAt() != null ? event.getChangedAt().toString() : "");
            
            // Determine notification title and message based on the change
            String title;
            String message;
            
            if ("SHARED".equals(event.getNewBankingModel())) {
                title = "Banking Model Changed to SHARED";
                message = String.format(
                    "Your chain '%s' has switched to SHARED banking model. " +
                    "All locations now use the headquarters payment account. " +
                    "Payments from all locations will be routed to the headquarters account.",
                    event.getChainName()
                );
            } else {
                title = "Banking Model Changed to INDIVIDUAL";
                message = String.format(
                    "Your chain '%s' has switched to INDIVIDUAL banking model. " +
                    "Each location now uses its own payment account. " +
                    "IMPORTANT: All active offers have been invalidated. " +
                    "Set up your individual payment account and reactivate your offers.",
                    event.getChainName()
                );
            }
            
            // Send individual notifications to all chain locations
            // Note: We send to ALL locations including the one who made the change,
            // so they can see the notification in their notification list as well
            if (event.getLocationVendorIds() != null && !event.getLocationVendorIds().isEmpty()) {
                int notificationCount = 0;
                int failedCount = 0;
                for (Long locationId : event.getLocationVendorIds()) {
                    try {
                        // Try to use BANKING_MODEL_CHANGED enum, fallback to SYSTEM_ALERT if not available
                        // This handles cases where the shared-entities JAR hasn't been updated yet
                        NotificationType notificationType;
                        try {
                            notificationType = NotificationType.valueOf("BANKING_MODEL_CHANGED");
                        } catch (IllegalArgumentException e) {
                            logger.warn("BANKING_MODEL_CHANGED enum not found, using SYSTEM_ALERT as fallback. " +
                                       "Please rebuild shared-entities and notification-service to use the correct enum.");
                            notificationType = NotificationType.SYSTEM_ALERT;
                        }
                        
                        NotificationRequestEvent notification = new NotificationRequestEvent(
                            locationId.toString(),  // Vendor ID as user ID (should match global user ID)
                            notificationType,
                            title,
                            message,
                            data
                        );
                        
                        logger.debug("Sending banking model change notification to location ID: {} (vendor ID: {})", 
                                    locationId, locationId);
                        notificationService.handleNotificationRequest(notification);
                        notificationCount++;
                        
                    } catch (Exception e) {
                        failedCount++;
                        logger.error("Failed to send banking model change notification to location ID {}: {}", 
                                    locationId, e.getMessage(), e);
                        // Continue with other locations even if one fails
                    }
                }
                logger.info("Sent {} banking model change notifications for chain: {} (total locations: {}, failed: {})", 
                          notificationCount, event.getChainName(), event.getLocationVendorIds().size(), failedCount);
            } else {
                logger.warn("No location vendor IDs provided in BankingModelChangedEvent for chain: {}", 
                           event.getChainName());
            }
            
        } catch (Exception e) {
            logger.error("Failed to process BankingModelChangedEvent for chain: {}", event.getChainName(), e);
        }
    }
}

