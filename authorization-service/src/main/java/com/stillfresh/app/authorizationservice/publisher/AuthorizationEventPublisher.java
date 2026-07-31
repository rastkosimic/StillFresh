package com.stillfresh.app.authorizationservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.shared.events.TokenValidationResponseEvent;
import com.stillfresh.app.sharedentities.user.events.LoggedUserEvent;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;
import com.stillfresh.app.sharedentities.vendor.events.LoggedVendorEvent;
import com.stillfresh.app.sharedentities.vendor.events.UpdateVendorProfileEvent;

@Service
public class AuthorizationEventPublisher {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthorizationEventPublisher.class);
	
    @Value("${user.topic.name:cache-logged-user}")
    private String cacheLoggedUserTopic;
    
    @Value("${vendor.topic.name:cache-logged-vendor}")
    private String cacheLoggedVendorTopic;
    
    @Value("${authorization.topic.name:token-validation-response}")
    private String tokenVaidationResponseTopic;
    
    @Value("${authorization.topic.password-update:password-update}")
    private String passwordUpdateTopic;

    @Value("${user.topic.name:user-profile-updated}")
    private String userProfileUpdatedTopic;

    @Value("${vendor.topic.name:vendor-profile-updated}")
    private String vendorProfileUpdatedTopic;
    
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuthorizationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void publishLoggedUserEvent(LoggedUserEvent event) {
        try {
        	logger.info("Published LoggedUserEvent to Kafka topic '{}'", cacheLoggedUserTopic);
            kafkaTemplate.send(cacheLoggedUserTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish LoggedUserEvent to Kafka", e);
        }
    }

	public void publishLoggedVendorEvent(LoggedVendorEvent event) {
        try {
        	logger.info("Published LoggedVendorEvent to Kafka topic '{}'", cacheLoggedVendorTopic);
            kafkaTemplate.send(cacheLoggedVendorTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish LoggedVendorEvent to Kafka", e);
        }
	}
	
	public void publishTokenValidationResponseEvent(TokenValidationResponseEvent event) {
        try {
        	logger.info("Published TokenValidationResponseEvent to Kafka topic '{}'", tokenVaidationResponseTopic);
            kafkaTemplate.send(tokenVaidationResponseTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish TokenValidationResponseEvent to Kafka", e);
        }
	}
	
	public void publishPasswordUpdateEvent(PasswordUpdateEvent event) {
        try {
        	logger.info("Published PasswordUpdateEvent to Kafka topic '{}' for user ID: {}", passwordUpdateTopic, event.getUserId());
            kafkaTemplate.send(passwordUpdateTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish PasswordUpdateEvent to Kafka for user ID: {}", event.getUserId(), e);
        }
	}

	public void publishUpdateUserProfileEvent(UpdateUserProfileEvent event) {
        try {
            logger.info("Published UpdateUserProfileEvent to Kafka topic '{}' for email: {}", userProfileUpdatedTopic, event.getEmail());
            kafkaTemplate.send(userProfileUpdatedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish UpdateUserProfileEvent to Kafka for email: {}", event.getEmail(), e);
        }
	}

	public void publishUpdateVendorProfileEvent(UpdateVendorProfileEvent event) {
        try {
            logger.info("Published UpdateVendorProfileEvent to Kafka topic '{}' for email: {}", vendorProfileUpdatedTopic, event.getEmail());
            kafkaTemplate.send(vendorProfileUpdatedTopic, event);
        } catch (Exception e) {
            logger.error("Failed to publish UpdateVendorProfileEvent to Kafka for email: {}", event.getEmail(), e);
        }
	}

}
