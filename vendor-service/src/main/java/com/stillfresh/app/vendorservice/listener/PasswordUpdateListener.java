package com.stillfresh.app.vendorservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;

@Component
public class PasswordUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUpdateListener.class);

    private final VendorRepository vendorRepository;

    public PasswordUpdateListener(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @KafkaListener(topics = "${authorization.topic.password-update:password-update}", groupId = "vendor-service-group")
    public void handlePasswordUpdate(PasswordUpdateEvent event) {
        logger.debug("Received PasswordUpdateEvent: {}", event);
        
        // Only process events for VENDOR or VENDOR_ADMIN roles
        if (event.getRole() != Role.VENDOR && event.getRole() != Role.VENDOR_ADMIN) {
            logger.debug("Ignoring PasswordUpdateEvent for non-VENDOR role: {}", event.getRole());
            return;
        }
        
        try {
            Vendor vendor = vendorRepository.findById(event.getUserId())
                .orElseThrow(() -> new RuntimeException("Vendor not found with ID: " + event.getUserId()));
            
            // Only update if password is different (idempotent operation)
            if (!vendor.getPassword().equals(event.getEncodedPassword())) {
                vendor.setPassword(event.getEncodedPassword());
                vendorRepository.save(vendor);
                logger.info("Password updated in vendor-service database for vendor ID: {}, email: {}", 
                           event.getUserId(), event.getEmail());
            } else {
                logger.debug("Password already matches in vendor-service for vendor ID: {}", event.getUserId());
            }
        } catch (Exception e) {
            logger.error("Failed to process PasswordUpdateEvent for vendor ID: {}, email: {}", 
                        event.getUserId(), event.getEmail(), e);
        }
    }
}

