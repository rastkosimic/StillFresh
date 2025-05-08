package com.stillfresh.app.offerservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.offer.events.OfferRequestEvent;

@Component
public class OfferRequestListener {

    @Autowired
    private OfferService offerService;

    private static final Logger logger = LoggerFactory.getLogger(OfferRequestListener.class);
    
    @KafkaListener(topics = "${offer.topic.offer-request:offer-request}", groupId = "offer-service-group")
    public void handleOfferRequestEvent(OfferRequestEvent event) {
        logger.info("Received OfferRequestEvent: {}", event);
        try {
            // Fetch nearby offers based on location and range
            offerService.findNearbyOffers(event.getLatitude(), event.getLongitude(), event.getRange(), event.getRequestId());
            logger.info("OfferRequestEvent processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process OfferRequestEvent", e);
        }
    }
}

