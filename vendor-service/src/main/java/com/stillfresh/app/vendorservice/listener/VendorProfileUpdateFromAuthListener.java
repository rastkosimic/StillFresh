package com.stillfresh.app.vendorservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.sharedentities.vendor.events.UpdateVendorProfileEvent;

/**
 * Listens for UpdateVendorProfileEvent from authorization-service (e.g. when a deleted vendor account is reactivated on login).
 * Updates the local vendor record so vendor-service stays in sync with auth.
 */
@Component
public class VendorProfileUpdateFromAuthListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorProfileUpdateFromAuthListener.class);

    private final VendorRepository vendorRepository;

    public VendorProfileUpdateFromAuthListener(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @KafkaListener(topics = "${vendor.topic.name:vendor-profile-updated}", groupId = "vendor-service-group")
    public void handleVendorProfileUpdateFromAuth(UpdateVendorProfileEvent event) {
        logger.debug("Received UpdateVendorProfileEvent (from auth): email={}", event.getEmail());
        try {
            vendorRepository.findByEmail(event.getEmail()).ifPresent(vendor -> {
                vendor.setUsername(event.getUsername());
                vendor.setEmail(event.getEmail());
                vendor.setPassword(event.getPassword());
                vendor.setRole(event.getRole());
                vendor.setStatus(event.getStatus());
                vendorRepository.save(vendor);
                logger.info("Updated vendor from auth event for email: {}, status: {}", event.getEmail(), event.getStatus());
            });
        } catch (Exception e) {
            logger.error("Failed to process UpdateVendorProfileEvent for email: {}", event.getEmail(), e);
        }
    }
}
