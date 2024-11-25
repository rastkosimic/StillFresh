package com.stillfresh.app.authorizationservice.listener.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.authorizationservice.service.UserService;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;

@Component
public class UserProfileUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileUpdateListener.class);

    private final UserService userService;

    public UserProfileUpdateListener (UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${user.topic.name:user-profile-updated}", groupId = "authorization-group")
    public void handleUserProfileUpdate(UpdateUserProfileEvent event) {
        logger.debug("Received UpdateUserProfileEvent: {}", event);
        try {
            userService.updateUser(event);
            logger.debug("UpdateUserProfileEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process UpdateUserProfileEvent for email: {}", event.getEmail(), e);
        }
    }
}
