package com.stillfresh.app.userservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.repository.UserRepository;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.user.events.PasswordUpdateEvent;

@Component
public class PasswordUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUpdateListener.class);

    private final UserRepository userRepository;

    public PasswordUpdateListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "${authorization.topic.password-update:password-update}", groupId = "user-service-group")
    public void handlePasswordUpdate(PasswordUpdateEvent event) {
        logger.debug("Received PasswordUpdateEvent: {}", event);
        
        // Only process events for USER role
        if (event.getRole() != Role.USER) {
            logger.debug("Ignoring PasswordUpdateEvent for non-USER role: {}", event.getRole());
            return;
        }
        
        try {
            User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + event.getUserId()));
            
            // Only update if password is different (idempotent operation)
            if (!user.getPassword().equals(event.getEncodedPassword())) {
                user.setPassword(event.getEncodedPassword());
                userRepository.save(user);
                logger.info("Password updated in user-service database for user ID: {}, email: {}", 
                           event.getUserId(), event.getEmail());
            } else {
                logger.debug("Password already matches in user-service for user ID: {}", event.getUserId());
            }
        } catch (Exception e) {
            logger.error("Failed to process PasswordUpdateEvent for user ID: {}, email: {}", 
                        event.getUserId(), event.getEmail(), e);
        }
    }
}

