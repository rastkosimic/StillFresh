package com.stillfresh.app.vendorservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.offer.events.AllOffersInvalidationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferCreationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferInvalidationEvent;
import com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.vendor.events.OfferRelatedVendorDetailsEvent;
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
    
    //-------offer topics--------
    @Value("${kafka.topic.offer-created:offer-created}")
    private String offerCreationTopic;
    
    @Value("${kafka.topic.offer-invalidated:offer-invalidated}")
    private String offerInvalidateTopic;

    @Value("${kafka.topic.all-offers-invalidated:all-offers-invalidated}")
    private String allOffersInvalidationTopic;
    
    @Value("${kafka.topic.update-offer:update-offer}")
    private String updateOfferTopic;
    
    @Value("${kafka.topic.update-vendor-related-offer-details:update-vendor-related-offer-details}")
    private String updateVendorRelatedOfferDetailsTopic;
    
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
	
    public void publishOfferCreationEvent(OfferCreationEvent event) {
        try {
            logger.info("Publishing OfferCreationEvent to Kafka topic '{}': {}", offerCreationTopic, event);
            kafkaTemplate.send(offerCreationTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferCreationEvent to Kafka", e);
        }
    }
    
    public void publishOfferInvalidationEvent(OfferInvalidationEvent event) {
        try {
            logger.info("Publishing OfferInvalidationEvent to Kafka topic '{}': {}", offerInvalidateTopic, event);
            kafkaTemplate.send(offerInvalidateTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferInvalidationEvent to Kafka", e);
        }
    }

	public void invalidateAllOffers(AllOffersInvalidationEvent event) {
        try {
            logger.info("Publishing AllOffersInvalidationEvent to Kafka topic '{}': {}", allOffersInvalidationTopic, event);
            kafkaTemplate.send(allOffersInvalidationTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish AllOffersInvalidationEvent to Kafka", e);
        }
		
	}

	public void publishUpdateOfferEvent(OfferUpdateEvent event) {
        try {
            logger.info("Publishing OfferUpdateEvent to Kafka topic '{}': {}", updateOfferTopic, event);
            kafkaTemplate.send(updateOfferTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferUpdateEvent to Kafka", e);
        }
	}

	public void publishOfferRelatedVendorDetailsEvent(OfferRelatedVendorDetailsEvent event) {
        try {
            logger.info("Publishing OfferRelatedVendorDetailsEvent to Kafka topic '{}': {}", updateVendorRelatedOfferDetailsTopic, event);
            kafkaTemplate.send(updateVendorRelatedOfferDetailsTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferRelatedVendorDetailsEvent to Kafka", e);
        }
		
	}
}
