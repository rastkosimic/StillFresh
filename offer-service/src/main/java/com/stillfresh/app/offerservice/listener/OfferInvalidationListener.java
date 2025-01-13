package com.stillfresh.app.offerservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.offer.events.OfferInvalidationEvent;

@Component
public class OfferInvalidationListener {
	
	private static final Logger logger = LoggerFactory.getLogger(OfferInvalidationListener.class);
	
	@Autowired
    private OfferService offerService;
	
    @KafkaListener(topics = "${kafka.topic.offer-invalidated:offer-invalidated}", groupId = "offer-service-group")
    public void handleOfferInvalidationRequestedEvent(OfferInvalidationEvent event) {
        logger.debug("Received OfferInvalidationEvent: {}", event);
        try {
        	offerService.invalidateOffer(event.getId());
            logger.debug("OfferInvalidationEvent processed successfully for offerId: {}", event.getId());
        } catch (Exception e) {
            logger.error("Failed to process OfferInvalidationEvent for offer: {}", event.getId(), e);
        }
    	
    }

}
