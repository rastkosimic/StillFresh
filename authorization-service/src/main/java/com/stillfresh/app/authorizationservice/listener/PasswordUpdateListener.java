package com.stillfresh.app.authorizationservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.authorizationservice.model.User;
import com.stillfresh.app.authorizationservice.repository.UserRepository;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;

@Component
public class PasswordUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUpdateListener.class);

    private final UserRepository userRepository;

    public PasswordUpdateListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "${authorization.topic.password-update:password-update}", groupId = "authorization-group")
    public void handlePasswordUpdate(PasswordUpdateEvent event) {
        logger.debug("Received PasswordUpdateEvent: {}", event);
        
        try {
            User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + event.getUserId()));
            
            // Only update if password is different (idempotent operation)
            if (!user.getPassword().equals(event.getEncodedPassword())) {
                user.setPassword(event.getEncodedPassword());
                userRepository.save(user);
                logger.info("Password updated in authorization-service database for user ID: {}, email: {}, role: {}", 
                           event.getUserId(), event.getEmail(), event.getRole());
            } else {
                logger.debug("Password already matches in authorization-service for user ID: {}", event.getUserId());
            }
        } catch (Exception e) {
            logger.error("Failed to process PasswordUpdateEvent for user ID: {}, email: {}", 
                        event.getUserId(), event.getEmail(), e);
        }
    }
}

