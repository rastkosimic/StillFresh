package com.stillfresh.app.notificationservice.service;

import com.stillfresh.app.notificationservice.client.AuthUserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resolves a recipient's email address from a userId via authorization-service.
 * Centralizes the lookup for email-sending consumers whose Kafka events only carry a userId.
 * Failures are swallowed (returns null) so that an outage never breaks the Kafka consumer;
 * the downstream {@link EmailService} treats a null recipient as a no-op.
 */
@Service
public class UserContactService {

    private static final Logger logger = LoggerFactory.getLogger(UserContactService.class);

    @Autowired
    private AuthUserClient authUserClient;

    /**
     * Resolves the email for the given user id, or null if it cannot be determined.
     */
    public String resolveEmail(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            Long globalUserId = Long.valueOf(userId.trim());
            Map<String, Object> response = authUserClient.getUserByGlobalId(globalUserId);
            if (response == null) {
                return null;
            }
            Object userObj = response.get("user");
            if (userObj instanceof Map<?, ?> user) {
                Object email = user.get("email");
                return email != null ? email.toString() : null;
            }
            return null;
        } catch (NumberFormatException e) {
            logger.warn("Cannot resolve email: userId '{}' is not numeric", userId);
            return null;
        } catch (Exception e) {
            logger.warn("Failed to resolve email for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
