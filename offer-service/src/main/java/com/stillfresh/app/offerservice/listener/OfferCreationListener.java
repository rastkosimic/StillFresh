package com.stillfresh.app.offerservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;

@Component
public class OfferCreationListener {
	
	private static final Logger logger = LoggerFactory.getLogger(OfferCreationListener.class);

    private final OfferService offerService;

    public OfferCreationListener(OfferService offerService) {
        this.offerService = offerService;
    }

    @KafkaListener(topics = "${kafka.topic.offer-created:offer-created}", groupId = "offer-service-group")
    public void handleOfferCreationRequestedEvent(OfferCreationEvent event) {
        logger.debug("Received OfferCreationEvent: {}", event);
        try {
        	offerService.createOffer(event);
            logger.debug("OfferCreationEvent processed successfully for offer: {}", event.getDescription());
        } catch (Exception e) {
            logger.error("Failed to process OfferCreationEvent for offer: {}", event.getDescription(), e);
        }
    	
    }
}

