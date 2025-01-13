package com.stillfresh.app.offerservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.offer.events.AllOffersInvalidationEvent;

@Component
public class AllOffersInvalidationListener {
	private static final Logger logger = LoggerFactory.getLogger(AllOffersInvalidationListener.class);
	
	@Autowired
    private OfferService offerService;
	
    @KafkaListener(topics = "${kafka.topic.all-offers-invalidated:all-offers-invalidated}", groupId = "offer-service-group")
    public void handleAllOffersInvalidationRequestedEvent(AllOffersInvalidationEvent event) {
        logger.debug("Received AllOffersInvalidationEvent: {}", event);
        try {
        	offerService.invalidateAllOffersByVendor(event.getId());
            logger.debug("AllOffersInvalidationEvent processed successfully for vendorId: {}", event.getId());
        } catch (Exception e) {
            logger.error("Failed to process AllOffersInvalidationEvent for vendorId: {}", event.getId(), e);
        }
    	
    }
}
