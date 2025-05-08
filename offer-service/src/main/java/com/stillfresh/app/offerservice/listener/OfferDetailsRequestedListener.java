package com.stillfresh.app.offerservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.offer.events.OfferDetailsRequestedEvent;

@Component
public class OfferDetailsRequestedListener {
	@Autowired
	private OfferService offerService;

	private static final Logger logger = LoggerFactory.getLogger(OfferDetailsRequestedListener.class);

	@KafkaListener(topics = "${offer.topic.offer-details-request:offer-details-request}", groupId = "offer-service-group")
	public void handleOfferDetailsRequestedEvent(OfferDetailsRequestedEvent event) {
		logger.info("Received OfferDetailsRequestedEvent for offerId: {}", event.getOfferId());
		try {
			offerService.respondToOfferDetailsRequest(event);
		} catch (Exception e) {
			logger.error("Failed to process OfferDetailsRequestedEvent for offerId: {}", event.getOfferId(), e);
		}
	}
}
