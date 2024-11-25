package com.stillfresh.app.authorizationservice.listener.vendor;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.vendor.events.VendorRegisteredEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VendorRegistrationListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorRegistrationListener.class);

    private final UserService userService;

    public VendorRegistrationListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${vendor.topic.name:vendor-registered}", groupId = "authorization-group")
    public void handleVendorRegistration(VendorRegisteredEvent event) {
        logger.debug("Received VendorRegisteredEvent: {}", event);
        try {
            userService.registerVendor(event);
            logger.debug("VendorRegisteredEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process VendorRegisteredEvent for email: {}", event.getEmail(), e);
        }
    }
}
