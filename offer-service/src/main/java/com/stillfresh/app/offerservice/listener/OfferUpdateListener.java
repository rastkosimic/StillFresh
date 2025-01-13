package com.stillfresh.app.offerservice.listener;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;
@Component
public class OfferUpdateListener {
	
	private static final Logger logger = LoggerFactory.getLogger(OfferUpdateListener.class);
	@Autowired
    private OfferService offerService;

    @KafkaListener(topics = "${kafka.topic.update-offer:update-offer}", groupId = "offer-service-group")
    public void handleOfferUpdateEvent(OfferUpdateEvent event) {
        logger.debug("Received OfferUpdateEvent: {}", event);
        try {
        	offerService.updateOffer(event);
            logger.debug("OfferUpdateEvent processed successfully for offer: {}", event.getDescription());
        } catch (Exception e) {
            logger.error("Failed to process OfferUpdateEvent for offer: {}", event.getDescription(), e);
        }
    	
    }

}
