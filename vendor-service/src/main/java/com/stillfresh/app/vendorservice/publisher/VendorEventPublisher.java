package com.stillfresh.app.vendorservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.vendor.events.UpdateVendorProfileEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorRegisteredEvent;
import com.stillfresh.app.sharedentities.vendor.events.VendorVerifiedEvent;

@Service
public class VendorEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(VendorEventPublisher.class);

    @Value("${vendor.topic.name:vendor-registered}")
    private String vendorTopic;
    
    @Value("${vendor.topic.name:vendor-verified}")
    private String vendorVerifiedTopic;
    
    @Value("${vendor.topic.name:vendor-profile-updated}")
    private String vendorProfileUpdateTopic;
    
    @Value("${authorization.topic.name:token-validation-request}")
    private String tokenValidationRequestTopic;
    
    @Value("${authorization.topic.name:token-invalidation-request}")
    private String tokenInvalidationRequestTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VendorEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishVendorRegisteredEvent(VendorRegisteredEvent event) {
        try {
        	logger.info("Published VendorRegisteredEvent to Kafka topic '{}'", vendorTopic);
            kafkaTemplate.send(vendorTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish VendorRegisteredEvent to Kafka", e);
        }
    }

	public void publishVendorVerifiedEvent(VendorVerifiedEvent event) {
        try {
        	logger.info("Published VendorVerifiedEvent to Kafka topic '{}'", vendorVerifiedTopic);
            kafkaTemplate.send(vendorVerifiedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish VendorVerifiedEvent to Kafka", e);
        }
		
	}

	public void publishUpdateVendorProfileEvent(UpdateVendorProfileEvent event) {
        try {
        	logger.info("Published UpdateVendorProfileEvent to Kafka topic '{}'", vendorProfileUpdateTopic);
            kafkaTemplate.send(vendorProfileUpdateTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish UpdateVendorProfileEvent to Kafka", e);
        }
		
	}
	
	public void publishTokenValidationRequest(TokenRequestEvent event) {
        try {
        	logger.info("Published TokenValidationEvent to Kafka topic '{}'", tokenValidationRequestTopic);
            kafkaTemplate.send(tokenValidationRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish TokenValidationEvent to Kafka", e);
        }
	}

	public void publishTokenInvalidationRequest(TokenRequestEvent event) {
        try {
        	logger.info("Published TokenRequestEvent for token invalidation to Kafka topic '{}'", tokenInvalidationRequestTopic);
            kafkaTemplate.send(tokenInvalidationRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish TokenValidationEvent to Kafka", e);
        }
		
	}
}
