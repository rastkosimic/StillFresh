package com.stillfresh.app.offerservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.offer.events.OfferQuantityUpdatedEvent;

@Component
public class OfferQuantityUpdatedListener {
	private static final Logger logger = LoggerFactory.getLogger(OfferQuantityUpdatedListener.class);
	@Autowired
    private OfferService offerService;

    @KafkaListener(topics = "${offer.topic.offer-quantity-updated:offer-quantity-updated}", groupId = "offer-service-group")
    public void handleOfferQuantityUpdatedEvent(OfferQuantityUpdatedEvent event) {
        logger.debug("Received OfferQuantityUpdatedEvent: {}", event);
        try {
        	offerService.updateOfferQuantity(event);
            logger.debug("OfferQuantityUpdatedEvent processed successfully for offer id: {}", event.getOfferId());
        } catch (Exception e) {
            logger.error("Failed to process OfferQuantityUpdatedEvent for offer id: {}", event.getOfferId(), e);
        }
    	
    }
}
