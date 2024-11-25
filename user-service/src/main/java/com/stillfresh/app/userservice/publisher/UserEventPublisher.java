package com.stillfresh.app.userservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;
import com.stillfresh.app.sharedentities.user.events.UserRegisteredEvent;
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
    
    @Value("${authorization.topic.name:token-validation-request}")
    private String tokenVaidationRequestTopic;
    
    @Value("${authorization.topic.name:token-invalidation-request}")
    private String tokenInvaidationRequestTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
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

	public void publishTokenValidationRequest(TokenRequestEvent event) {
        try {
        	logger.info("Published TokenValidationEvent to Kafka topic '{}'", tokenVaidationRequestTopic);
            kafkaTemplate.send(tokenVaidationRequestTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish TokenValidationEvent to Kafka", e);
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
}
