package com.stillfresh.app.userservice.service;

import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.user.events.UserSuspendedEvent;
import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.publisher.UserEventPublisher;
import com.stillfresh.app.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks anti-abuse strikes (bypass attempts and no-shows) and auto-suspends a user once the
 * combined active-strike threshold is reached. Suspension publishes a {@link UserSuspendedEvent}
 * so authorization-service can block re-login and revoke active sessions.
 */
@Service
public class StrikeService {

    private static final Logger logger = LoggerFactory.getLogger(StrikeService.class);

    @Value("${strikes.suspension-threshold:3}")
    private int suspensionThreshold;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEventPublisher eventPublisher;

    @Transactional
    public void recordBypassStrike(Long userId, String reason) {
        userRepository.findById(userId).ifPresentOrElse(user -> {
            user.setBypassStrikeCount(user.getBypassStrikeCount() + 1);
            userRepository.save(user);
            logger.info("Recorded bypass strike for user {} (bypass={}, no-show={})",
                    userId, user.getBypassStrikeCount(), user.getNoShowStrikeCount());
            suspendIfThresholdReached(user, reason != null ? reason : "Bypass strike threshold reached");
        }, () -> logger.warn("Cannot record bypass strike: user {} not found", userId));
    }

    @Transactional
    public void recordNoShowStrike(Long userId) {
        userRepository.findById(userId).ifPresentOrElse(user -> {
            user.setNoShowStrikeCount(user.getNoShowStrikeCount() + 1);
            userRepository.save(user);
            logger.info("Recorded no-show strike for user {} (bypass={}, no-show={})",
                    userId, user.getBypassStrikeCount(), user.getNoShowStrikeCount());
            suspendIfThresholdReached(user, "No-show strike threshold reached");
        }, () -> logger.warn("Cannot record no-show strike: user {} not found", userId));
    }

    private void suspendIfThresholdReached(User user, String reason) {
        int activeStrikes = user.getBypassStrikeCount() + user.getNoShowStrikeCount();
        if (activeStrikes >= suspensionThreshold && user.getStatus() != Status.SUSPENDED) {
            user.setStatus(Status.SUSPENDED);
            userRepository.save(user);
            logger.warn("User {} auto-suspended after {} active strikes", user.getId(), activeStrikes);
            eventPublisher.publishUserSuspendedEvent(new UserSuspendedEvent(user.getId(), reason));
        }
    }
}
