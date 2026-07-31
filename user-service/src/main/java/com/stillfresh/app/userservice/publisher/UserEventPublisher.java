package com.stillfresh.app.userservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.offer.events.OfferRequestEvent;
import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.UpdatePaymentServiceEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;
import com.stillfresh.app.sharedentities.user.events.UserRegisteredEvent;
import com.stillfresh.app.sharedentities.user.events.UserSuspendedEvent;
import com.stillfresh.app.sharedentities.user.events.UserVerifiedEvent;

@Service
public class UserEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserEventPublisher.class);

    @Value("${user.topic.name:user-registered}")
    private String userTopic;
    
    @Value("${user.topic.name:user-verified}")
    private String userVerifiedTopic;
    
    @Value("${user.topic.name:user-profile-updated}")
    private String userProfileUpdateTopic;
    
    @Value("${authorization.topic.name:token-invalidation-request}")
    private String tokenInvaidationRequestTopic;
    
    @Value("${authorization.topic.password-update:password-update}")
    private String passwordUpdateTopic;

    @Value("${user.topic.user-suspended:user-suspended}")
    private String userSuspendedTopic;
    
    //--------offer topics-----------
    @Value("${offer.topic.offer-request:offer-request}")
    private String offerRequestTopic;
    
    
    //--------order topics-----------
    @Value("${order.topic.order-request:order-request}")
    private String orderRequestTopic;
    
    //--------payment topics-----------
    @Value("${payment.topic.name:payment-service-update}")
    private String paymentServiceUpdateTopic;

	@Autowired    
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        try {
        	logger.info("Published UserRegisteredEvent to Kafka topic '{}'", userTopic);
            kafkaTemplate.send(userTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish UserRegisteredEvent to Kafka", e);
        }
    }

	public void publishUserVerifiedEvent(UserVerifiedEvent event) {
        try {
        	logger.info("Published UserVerifiedEvent to Kafka topic '{}'", userVerifiedTopic);
            kafkaTemplate.send(userVerifiedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish UserVerifiedEvent to Kafka", e);
        }
		
	}

	public void publishUpdateUserProfileEvent(UpdateUserProfileEvent event) {
        try {
        	logger.info("Published UpdateUserProfileEvent to Kafka topic '{}'", userProfileUpdateTopic);
            kafkaTemplate.send(userProfileUpdateTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish UpdateUserProfileEvent to Kafka", e);
        }
		
	}

	public void publishTokenInvalidationRequest(TokenRequestEvent event) {
        try {
        	logger.info("Published TokenRequestEvent to Kafka topic '{}'", tokenInvaidationRequestTopic);
            kafkaTemplate.send(tokenInvaidationRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish TokenRequestEvent to Kafka", e);
        }
	}

	
	//---------------Offer related events-------------------
	
	public void publishOfferRequestEvent(OfferRequestEvent event) {
        try {
        	logger.info("Published OfferRequestEvent to Kafka topic '{}'", offerRequestTopic);
            kafkaTemplate.send(offerRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish OfferRequestEvent to Kafka", e);
        }
		
	}

	//---------------Order related events-------------------
	public void publishOrderRequestEvent(OrderRequestEvent event) {
	      try {
	    	logger.info("Published OrderRequestEvent to Kafka topic '{}'", orderRequestTopic);
	        kafkaTemplate.send(orderRequestTopic, event);
	    } catch (Exception e) {
	        logger.error("Failed to publish OrderRequestEvent to Kafka", e);
	    }
		
	}

	//---------------Payment related events-------------------	
	public void publishPaymentServiceUpdateEvent(UpdatePaymentServiceEvent event) {
	      try {
	    	logger.info("Published UpdatePaymentServiceEvent to Kafka topic '{}'", paymentServiceUpdateTopic);
	        kafkaTemplate.send(paymentServiceUpdateTopic, event);
	    } catch (Exception e) {
	        logger.error("Failed to publish UpdatePaymentServiceEvent to Kafka", e);
	    }
	}
	
	//---------------User suspension event-------------------
	public void publishUserSuspendedEvent(UserSuspendedEvent event) {
        try {
        	logger.info("Published UserSuspendedEvent to Kafka topic '{}' for user ID: {}", userSuspendedTopic, event.getUserId());
            kafkaTemplate.send(userSuspendedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish UserSuspendedEvent to Kafka for user ID: {}", event.getUserId(), e);
        }
	}

	//---------------Password update event-------------------
	public void publishPasswordUpdateEvent(PasswordUpdateEvent event) {
        try {
        	logger.info("Published PasswordUpdateEvent to Kafka topic '{}' for user ID: {}", passwordUpdateTopic, event.getUserId());
            kafkaTemplate.send(passwordUpdateTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish PasswordUpdateEvent to Kafka for user ID: {}", event.getUserId(), e);
        }
	}
}
