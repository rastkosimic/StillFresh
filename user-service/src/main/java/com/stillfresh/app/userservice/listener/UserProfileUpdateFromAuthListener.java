package com.stillfresh.app.userservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.repository.UserRepository;
import com.stillfresh.app.sharedentities.user.events.UpdateUserProfileEvent;

/**
 * Listens for UpdateUserProfileEvent from authorization-service (e.g. when a deleted account is reactivated on login).
 * Updates the local user record so user-service stays in sync with auth.
 */
@Component
public class UserProfileUpdateFromAuthListener {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileUpdateFromAuthListener.class);

    private final UserRepository userRepository;

    public UserProfileUpdateFromAuthListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "${user.topic.name:user-profile-updated}", groupId = "user-service-group")
    public void handleUserProfileUpdateFromAuth(UpdateUserProfileEvent event) {
        logger.debug("Received UpdateUserProfileEvent (from auth): email={}", event.getEmail());
        try {
            userRepository.findByEmail(event.getEmail()).ifPresent(user -> {
                user.setUsername(event.getUsername());
                user.setEmail(event.getEmail());
                user.setPassword(event.getPassword());
                user.setRole(event.getRole());
                user.setStatus(event.getStatus());
                userRepository.save(user);
                logger.info("Updated user from auth event for email: {}, status: {}", event.getEmail(), event.getStatus());
            });
        } catch (Exception e) {
            logger.error("Failed to process UpdateUserProfileEvent for email: {}", event.getEmail(), e);
        }
    }
}
