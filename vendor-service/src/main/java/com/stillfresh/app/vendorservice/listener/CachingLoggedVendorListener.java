package com.stillfresh.app.vendorservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.vendor.events.LoggedVendorEvent;
import com.stillfresh.app.vendorservice.service.VendorService;

@Component
public class CachingLoggedVendorListener {
	
    private static final Logger logger = LoggerFactory.getLogger(CachingLoggedVendorListener.class);

    private final VendorService vendorService;

    public CachingLoggedVendorListener(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @KafkaListener(topics = "${vendor.topic.name:cache-logged-user}", groupId = "authorization-group")
    public void handleVendorCaching(LoggedVendorEvent event) {
        logger.debug("Received LoggedVendorEvent: {}", event);
        try {
        	vendorService.cacheVendorOnLogin(event.getEmail());
            logger.debug("LoggedVendorEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process LoggedVendorEvent for email: {}", event.getEmail(), e);
        }
    }

}
