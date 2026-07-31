package com.stillfresh.app.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class OfferNotificationConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(OfferNotificationConsumer.class);
    
    @KafkaListener(topics = "${offer.topic.offer-created:offer-created}", 
                   groupId = "notification-service")
    public void handleOfferCreationEvent(OfferCreationEvent event) {
        try {
            logger.info("Received offer creation event for vendor: {}", event.getVendorId());
            
            // Notify users about new offers (you might want to filter by location/preferences)
            // This could be implemented with user preferences service
            Map<String, String> data = new HashMap<>();
            data.put("vendorId", String.valueOf(event.getVendorId()));
            data.put("offerName", event.getName());
            
            // For now, we'll just log - in production, you'd query users interested in this type of offer
            logger.info("New offer created: {} by vendor {}", event.getName(), event.getVendorId());
            
        } catch (Exception e) {
            logger.error("Failed to process offer creation event", e);
        }
    }
    
    @KafkaListener(topics = "${offer.topic.offer-updated:offer-updated}", 
                   groupId = "notification-service")
    public void handleOfferUpdateEvent(OfferUpdateEvent event) {
        try {
            logger.info("Received offer update event for offer: {}", event.getOfferId());
            
            // Notify users about offer updates
            Map<String, String> data = new HashMap<>();
            data.put("offerId", String.valueOf(event.getOfferId()));
            data.put("vendorId", String.valueOf(event.getVendorId()));
            data.put("offerName", event.getName());
            
            logger.info("Offer updated: {} by vendor {}", event.getName(), event.getVendorId());
            
        } catch (Exception e) {
            logger.error("Failed to process offer update event", e);
        }
    }
}
