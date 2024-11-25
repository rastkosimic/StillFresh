package com.stillfresh.app.authorizationservice.listener.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.user.events.UserRegisteredEvent;

@Component
public class UserRegistrationListener {

    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationListener.class);

    private final UserService userService;

    public UserRegistrationListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${user.topic.name:user-registered}", groupId = "authorization-group")
    public void handleUserRegistration(UserRegisteredEvent event) {
        logger.debug("Received UserRegisteredEvent: {}", event);
        try {
            userService.registerUser(event);
            logger.debug("UserRegisteredEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process UserRegisteredEvent for email: {}", event.getEmail(), e);
        }
    }
}