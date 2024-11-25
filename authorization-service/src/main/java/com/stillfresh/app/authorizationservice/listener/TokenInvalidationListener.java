package com.stillfresh.app.authorizationservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;

public class TokenInvalidationListener {
    private static final Logger logger = LoggerFactory.getLogger(TokenInvalidationListener.class);

    private final UserService userService;

    public TokenInvalidationListener (UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${authorization.topic.name:token-invalidation-request}")
    public void handleTokenValidation(TokenRequestEvent event) {
        logger.debug("Received TokenRequestEvent: {}", event);
        try {
            userService.logoutAndInvalidateToken(event.getToken());
            logger.debug("Token invalidation processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process token invalidation", e);
        }
    }
    
//    @KafkaListener(topics = "${authorization.topic.name:token-validation-request}", groupId = "vendor-service-group")
//    public void handleVendorTokenValidation(TokenRequestEvent event) {
//        logger.debug("Received TokenValidationEvent: {}", event);
//        try {
//            userService.tokenValidation(event);
//            logger.debug("TokenValidationEvent processed successfully");
//        } catch (Exception e) {
//            logger.error("Failed to process TokenValidationEvent", e);
//        }
//    }
}
