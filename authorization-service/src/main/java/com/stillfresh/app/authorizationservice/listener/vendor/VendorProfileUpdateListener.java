package com.stillfresh.app.authorizationservice.listener.vendor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.vendor.events.UpdateVendorProfileEvent;

@Component
public class VendorProfileUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorProfileUpdateListener.class);

    private final UserService userService;

    public VendorProfileUpdateListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${vendor.topic.name:vendor-profile-updated}", groupId = "authorization-group")
    public void handleVendorProfileUpdate(UpdateVendorProfileEvent event) {
        logger.debug("Received UpdateVendorProfileEvent: {}", event);
        try {
            userService.updateVendor(event);
            logger.debug("UpdateVendorProfileEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process UpdateVendorProfileEvent for email: {}", event.getEmail(), e);
        }
    }
}
