package com.stillfresh.app.userservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.user.events.LoggedUserEvent;
import com.stillfresh.app.userservice.service.UserService;

@Component
public class CachingLoggedUserListener {
	
    private static final Logger logger = LoggerFactory.getLogger(CachingLoggedUserListener.class);

    private final UserService userService;

    public CachingLoggedUserListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "${user.topic.name:cache-logged-user}", groupId = "authorization-group")
    public void handleUserCaching(LoggedUserEvent event) {
        logger.info("Received LoggedUserEvent: {}", event);
        try {
            userService.cacheUserOnLogin(event.getEmail());
            logger.info("LoggedUserEvent processed successfully for email: {}", event.getEmail());
        } catch (Exception e) {
            logger.error("Failed to process LoggedUserEvent for email: {}", event.getEmail(), e);
        }
    }

}
