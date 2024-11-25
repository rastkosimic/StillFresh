package com.stillfresh.app.authorizationservice.listener.vendor;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.vendor.events.VendorRegisteredEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorVerifiedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VendorVerificationListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorRegistrationListener.class);

    private final UserService userService;

    public VendorVerificationListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${vendor.topic.name:vendor-verified}", groupId = "authorization-group")
    public void handleVendorVerification(VendorVerifiedEvent event) {
        logger.debug("Received VendorVerifiedEvent: {}", event);
        try {
            userService.verifyVendor(event);
            logger.debug("VendorVerifiedEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process VendorVerifiedEvent for email: {}", event.getEmail(), e);
        }
    }
}
