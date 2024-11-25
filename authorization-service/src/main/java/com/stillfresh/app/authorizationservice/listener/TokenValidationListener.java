package com.stillfresh.app.authorizationservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;

@Component
public class TokenValidationListener {
    private static final Logger logger = LoggerFactory.getLogger(TokenValidationListener.class);

    private final UserService userService;

    public TokenValidationListener (UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${authorization.topic.name:token-validation-request}", groupId = "authorization-group")
    public void handleTokenValidation(TokenRequestEvent event) {
        logger.debug("Received TokenValidationEvent: {}", event);
        try {
            userService.tokenValidation(event);
            logger.debug("TokenValidationEvent processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process TokenValidationEvent", e);
        }
    }
    
    @KafkaListener(topics = "${authorization.topic.name:token-validation-request}", groupId = "vendor-service-group")
    public void handleVendorTokenValidation(TokenRequestEvent event) {
        logger.debug("Received TokenValidationEvent: {}", event);
        try {
            userService.tokenValidation(event);
            logger.debug("TokenValidationEvent processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process TokenValidationEvent", e);
        }
    }
    
}
