package com.stillfresh.app.authorizationservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.shared.events.TokenValidationResponseEvent;
import com.stillfresh.app.sharedentities.user.events.LoggedUserEvent;
import com.stillfresh.app.sharedentities.vendor.events.LoggedVendorEvent;

@Service
public class AuthorizationEventPublisher {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthorizationEventPublisher.class);
	
    @Value("${user.topic.name:cache-logged-user}")
    private String cacheLoggedUserTopic;
    
    @Value("${vendor.topic.name:cache-logged-vendor}")
    private String cacheLoggedVendorTopic;
    
    @Value("${authorization.topic.name:token-validation-response}")
    private String tokenVaidationResponseTopic;
    
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

}
