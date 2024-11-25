package com.stillfresh.app.authorizationservice.listener.user;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.user.events.UserVerifiedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserVerificationListener {

    private static final Logger logger = LoggerFactory.getLogger(UserVerificationListener.class);

    private final UserService userService;

    public UserVerificationListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${user.topic.name:user-verified}", groupId = "authorization-group")
    public void handleUserVerification(UserVerifiedEvent event) {
        logger.debug("Received UserVerifiedEvent: {}", event);
        try {
            userService.verifyUser(event);
            logger.debug("UserVerifiedEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process UserVerifiedEvent for email: {}", event.getEmail(), e);
        }
    }
}
