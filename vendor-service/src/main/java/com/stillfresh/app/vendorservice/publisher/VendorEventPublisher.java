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
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutDataResponseEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutStatusUpdateResponseEvent;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoResponseEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.order.events.VendorStatsRequestEvent;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;
import com.stillfresh.app.sharedentities.vendor.events.BankingModelChangedEvent;
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

    @Value("${kafka.topic.vendor-stats-request:vendor-stats-request}")
    private String vendorStatsRequestTopic;
    
    @Value("${payment.topic.mor-payout-data-response:mor-payout-data-response}")
    private String morPayoutDataResponseTopic;
    
    @Value("${payment.topic.mor-payout-status-update-response:mor-payout-status-update-response}")
    private String morPayoutStatusUpdateResponseTopic;
    
    @Value("${payment.topic.vendor-payment-info-response:vendor-payment-info-response}")
    private String vendorPaymentInfoResponseTopic;
    
    @Value("${vendor.topic.banking-model-changed:banking-model-changed}")
    private String bankingModelChangedTopic;
    
    @Value("${authorization.topic.password-update:password-update}")
    private String passwordUpdateTopic;
    
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

    public void publishVendorStatsRequest(VendorStatsRequestEvent event) {
        try {
            logger.info("Publishing VendorStatsRequestEvent to Kafka topic '{}': {}", vendorStatsRequestTopic, event);
            kafkaTemplate.send(vendorStatsRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish VendorStatsRequestEvent to Kafka", e);
        }
    }

    public void publishMoRPayoutDataResponse(MoRPayoutDataResponseEvent event) {
        try {
            logger.info("Publishing MoRPayoutDataResponseEvent to Kafka topic '{}': requestId={}, type={}", 
                       morPayoutDataResponseTopic, event.getRequestId(), event.getRequestType());
            kafkaTemplate.send(morPayoutDataResponseTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish MoRPayoutDataResponseEvent to Kafka", e);
        }
    }

    public void publishMoRPayoutStatusUpdateResponse(MoRPayoutStatusUpdateResponseEvent event) {
        try {
            logger.info("Publishing MoRPayoutStatusUpdateResponseEvent to Kafka topic '{}': requestId={}, payoutId={}", 
                       morPayoutStatusUpdateResponseTopic, event.getRequestId(), event.getPayoutId());
            kafkaTemplate.send(morPayoutStatusUpdateResponseTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish MoRPayoutStatusUpdateResponseEvent to Kafka", e);
        }
    }

    public void publishVendorPaymentInfoResponse(VendorPaymentInfoResponseEvent event) {
        try {
            logger.info("Publishing VendorPaymentInfoResponseEvent to Kafka topic '{}': requestId={}, vendorId={}, payoutModel={}", 
                       vendorPaymentInfoResponseTopic, event.getRequestId(), event.getVendorId(), event.getPayoutModel());
            kafkaTemplate.send(vendorPaymentInfoResponseTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish VendorPaymentInfoResponseEvent to Kafka", e);
        }
    }

    public void publishBankingModelChangedEvent(BankingModelChangedEvent event) {
        try {
            logger.info("Publishing BankingModelChangedEvent to Kafka topic '{}': chainId={}, chainName={}, newModel={}", 
                       bankingModelChangedTopic, event.getChainId(), event.getChainName(), event.getNewBankingModel());
            kafkaTemplate.send(bankingModelChangedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish BankingModelChangedEvent to Kafka", e);
        }
    }
    
    public void publishPasswordUpdateEvent(PasswordUpdateEvent event) {
        try {
            logger.info("Published PasswordUpdateEvent to Kafka topic '{}' for vendor ID: {}", passwordUpdateTopic, event.getUserId());
            kafkaTemplate.send(passwordUpdateTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish PasswordUpdateEvent to Kafka for vendor ID: {}", event.getUserId(), e);
        }
    }
}
