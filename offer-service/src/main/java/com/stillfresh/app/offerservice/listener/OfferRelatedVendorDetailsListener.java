package com.stillfresh.app.offerservice.listener;

import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;
import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
@Component
public class OfferRelatedVendorDetailsListener {
	private static final Logger logger = LoggerFactory.getLogger(OfferRelatedVendorDetailsListener.class);
	@Autowired
    private OfferService offerService;

    @KafkaListener(topics = "${kafka.topic.update-vendor-related-offer-details:update-vendor-related-offer-details}", groupId = "offer-service-group")
    public void handleOfferRelatedVendorDetailsEvent(OfferRelatedVendorDetailsEvent event) {
        logger.debug("Received OfferRelatedVendorDetailsEvent: {}", event);
        try {
        	offerService.updateOfferRelatedVendorDetails(event);
            logger.debug("OfferRelatedVendorDetailsEvent processed successfully for vendor: {}", event.getId());
        } catch (Exception e) {
            logger.error("Failed to process OfferRelatedVendorDetailsEvent for vendor: {}", event.getId(), e);
        }
    	
    }
}
