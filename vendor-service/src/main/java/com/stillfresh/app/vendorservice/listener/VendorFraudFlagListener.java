package com.stillfresh.app.vendorservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.sharedentities.order.events.FraudFlagEvent;

/**
 * Increments a vendor's bypass strike counter when a customer cancellation at the pickup location is
 * flagged as a potential user-vendor bypass. Used by ops to monitor colluding vendors.
 */
@Component
public class VendorFraudFlagListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorFraudFlagListener.class);

    private final VendorRepository vendorRepository;

    public VendorFraudFlagListener(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @KafkaListener(topics = "${order.topic.fraud-flag:fraud-flag}", groupId = "vendor-service-group")
    @Transactional
    public void handleFraudFlag(FraudFlagEvent event) {
        logger.info("Received FraudFlagEvent for vendor {} (order {}): {}",
                event.getVendorId(), event.getOrderId(), event.getReason());
        if (event.getVendorId() == null) {
            return;
        }
        try {
            vendorRepository.findById(event.getVendorId()).ifPresentOrElse(vendor -> {
                vendor.setBypassStrikeCount(vendor.getBypassStrikeCount() + 1);
                vendorRepository.save(vendor);
                logger.warn("Vendor {} bypass strike count incremented to {}",
                        vendor.getId(), vendor.getBypassStrikeCount());
            }, () -> logger.warn("Cannot record vendor bypass strike: vendor {} not found", event.getVendorId()));
        } catch (Exception e) {
            logger.error("Failed to record bypass strike for vendor {}", event.getVendorId(), e);
        }
    }
}
