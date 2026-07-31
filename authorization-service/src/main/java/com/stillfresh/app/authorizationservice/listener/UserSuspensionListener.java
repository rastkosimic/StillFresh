package com.stillfresh.app.authorizationservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.authorizationservice.repository.UserRepository;
import com.stillfresh.app.authorizationservice.service.RefreshTokenService;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.user.events.UserSuspendedEvent;

/**
 * On user suspension (from user-service): blocks re-login by setting the auth-side status to
 * {@code SUSPENDED} and revokes all active refresh tokens so existing sessions cannot be rotated.
 */
@Component
public class UserSuspensionListener {

    private static final Logger logger = LoggerFactory.getLogger(UserSuspensionListener.class);

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public UserSuspensionListener(UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @KafkaListener(topics = "${user.topic.user-suspended:user-suspended}", groupId = "authorization-group")
    public void handleUserSuspended(UserSuspendedEvent event) {
        logger.warn("Received UserSuspendedEvent for user {}: {}", event.getUserId(), event.getReason());
        if (event.getUserId() == null) {
            return;
        }
        try {
            userRepository.findById(event.getUserId()).ifPresent(user -> {
                if (user.getStatus() != Status.SUSPENDED) {
                    user.setStatus(Status.SUSPENDED);
                    userRepository.save(user);
                    logger.info("User {} marked SUSPENDED in authorization-service", event.getUserId());
                }
            });
            // Revoke all sessions regardless of whether the local row existed.
            refreshTokenService.revokeAllSessionsForUser(event.getUserId());
            logger.info("Revoked all refresh-token sessions for suspended user {}", event.getUserId());
        } catch (Exception e) {
            logger.error("Failed to process UserSuspendedEvent for user {}", event.getUserId(), e);
        }
    }
}
