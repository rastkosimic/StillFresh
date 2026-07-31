package com.stillfresh.app.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
import com.stillfresh.app.notificationservice.config.NotificationMetrics;
import com.stillfresh.app.notificationservice.model.NotificationEntity;
import com.stillfresh.app.notificationservice.model.NotificationStatus;
import com.stillfresh.app.notificationservice.publisher.NotificationEventPublisher;
import com.stillfresh.app.notificationservice.repository.FcmTokenRepository;
import com.stillfresh.app.notificationservice.repository.NotificationRepository;
import com.stillfresh.app.sharedentities.notification.events.NotificationFailedEvent;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.notification.events.NotificationSentEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private FcmTokenRepository fcmTokenRepository;
    
    @Autowired
    private NotificationMetrics metrics;

    @Autowired
    private NotificationPreferencesService preferencesService;

    @Value("${notification.retention-days:90}")
    private int retentionDays;

    public void handleNotificationRequest(NotificationRequestEvent event) {
        try {
            OffsetDateTime now = OffsetDateTime.now();

            NotificationEntity notification = new NotificationEntity();
            notification.setUserId(event.getUserId());
            notification.setType(event.getType());
            notification.setTitle(event.getTitle());
            notification.setMessage(event.getMessage());
            notification.setData(objectMapper.valueToTree(event.getData()));
            notification.setStatus(NotificationStatus.PENDING);
            notification.setCreatedAt(now);

            notification = notificationRepository.save(notification);
            
            logger.info("Saved notification for user ID: {}, type: {}, title: {}, notification ID: {}", 
                       event.getUserId(), event.getType(), event.getTitle(), notification.getId());
            
            sendPushNotification(notification);

        } catch (Exception e) {
            logger.error("Failed to process notification request for user ID: {}, type: {}", 
                        event.getUserId(), event.getType(), e);
            eventPublisher.publishNotificationFailed(
                new NotificationFailedEvent(
                    event.getNotificationId(),
                    event.getUserId(),
                    e.getMessage(),
                    OffsetDateTime.now()
                )
            );
        }
    }

    @Retryable(value = {FirebaseMessagingException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @Transactional
    private void sendPushNotification(NotificationEntity notification) {
        // Respect user preferences: the in-app record is always persisted, but the push
        // is only delivered when the user has push enabled for this notification type.
        if (!isPushAllowed(notification)) {
            logger.info("Push delivery skipped for user {} (type {}): disabled by user preferences",
                    notification.getUserId(), notification.getType());
            notification.setStatus(NotificationStatus.SKIPPED);
            notification.setSentAt(OffsetDateTime.now());
            notificationRepository.save(notification);
            return;
        }

        try {
            String token = getUserFcmToken(notification.getUserId());

            Notification firebaseNotification = Notification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getMessage())
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setNotification(firebaseNotification)
                .setToken(token);

            if (notification.getData() != null) {
                notification.getData().fields().forEachRemaining(entry ->
                    messageBuilder.putData(entry.getKey(), entry.getValue().asText())
                );
            }

            String response = firebaseMessaging.send(messageBuilder.build());
            logger.info("Notification sent: {}", response);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
            notificationRepository.save(notification);
            
            // Increment metrics
            metrics.notificationSentCounter().increment();

            eventPublisher.publishNotificationSent(
                new NotificationSentEvent(
                    notification.getId(),
                    notification.getUserId(),
                    notification.getType(),
                    notification.getSentAt()
                )
            );

        } catch (IllegalStateException e) {
            // Handle case where user doesn't have an FCM token registered
            logger.warn("Cannot send notification to user {}: {}", notification.getUserId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setError(e.getMessage());
            notification.setSentAt(OffsetDateTime.now());
            notificationRepository.save(notification);
            
            // Increment metrics
            metrics.notificationFailedCounter().increment();

            eventPublisher.publishNotificationFailed(
                new NotificationFailedEvent(
                    notification.getId(),
                    notification.getUserId(),
                    e.getMessage(),
                    notification.getSentAt()
                )
            );
        } catch (FirebaseMessagingException e) {
            logger.error("Firebase error while sending notification", e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setError(e.getMessage());
            notification.setSentAt(OffsetDateTime.now());
            notificationRepository.save(notification);
            
            // Increment metrics
            metrics.notificationFailedCounter().increment();

            eventPublisher.publishNotificationFailed(
                new NotificationFailedEvent(
                    notification.getId(),
                    notification.getUserId(),
                    e.getMessage(),
                    notification.getSentAt()
                )
            );
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void processPendingNotifications() {
        List<NotificationEntity> pending = notificationRepository.findByStatus(NotificationStatus.PENDING);
        for (NotificationEntity notification : pending) {
            sendPushNotification(notification);
        }
    }

    public List<NotificationEntity> getUserNotifications(String userId) {
        logger.debug("Retrieving notifications for user ID: {}", userId);
        List<NotificationEntity> notifications = notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        logger.debug("Found {} notifications for user ID: {}", notifications.size(), userId);
        return notifications;
    }
    
    public List<NotificationEntity> getUnreadNotifications(String userId) {
        logger.debug("Retrieving unread notifications for user ID: {}", userId);
        List<NotificationEntity> notifications = notificationRepository.findByUserIdAndIsReadAndDeletedFalseOrderByCreatedAtDesc(userId, false);
        logger.debug("Found {} unread notifications for user ID: {}", notifications.size(), userId);
        return notifications;
    }
    
    public void markAsRead(String notificationId) {
        try {
            UUID id = UUID.fromString(notificationId);
            notificationRepository.findById(id).ifPresent(notification -> {
                notification.setRead(true);
                notification.setStatus(NotificationStatus.READ);
                notificationRepository.save(notification);
            });
        } catch (IllegalArgumentException e) {
            logger.error("Invalid notification ID format: {}", notificationId);
        }
    }
    
    public void markAllAsRead(String userId) {
        List<NotificationEntity> notifications = notificationRepository.findByUserIdAndIsReadAndDeletedFalseOrderByCreatedAtDesc(userId, false);
        notifications.forEach(notification -> {
            notification.setRead(true);
            notification.setStatus(NotificationStatus.READ);
        });
        notificationRepository.saveAll(notifications);
    }

    /**
     * Soft-delete a notification (hide from user listing). Only the owner can delete.
     * @return true if the notification was found, owned by user, and marked deleted; false otherwise
     */
    @Transactional
    public boolean deleteNotification(String notificationId, String userId) {
        try {
            UUID id = UUID.fromString(notificationId);
            return notificationRepository.findById(id)
                    .filter(n -> n.getUserId().equals(userId))
                    .map(notification -> {
                        notification.setDeleted(true);
                        notificationRepository.save(notification);
                        logger.debug("Soft-deleted notification {} for user {}", notificationId, userId);
                        return true;
                    })
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid notification ID format: {}", notificationId);
            return false;
        }
    }
    
    public void sendTestNotification(String userId) {
        Map<String, String> data = new HashMap<>();
        data.put("test", "true");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        NotificationRequestEvent notificationRequest = new NotificationRequestEvent(
            userId,
            com.stillfresh.app.sharedentities.enums.NotificationType.SYSTEM_ALERT,
            "Test Notification",
            "This is a test notification from StillFresh!",
            data
        );
        
        handleNotificationRequest(notificationRequest);
    }
    
    public boolean isNotificationOwnedByUser(String notificationId, String userId) {
        try {
            UUID id = UUID.fromString(notificationId);
            return notificationRepository.findById(id)
                    .map(n -> n.getUserId().equals(userId))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid notification ID format: {}", notificationId);
            return false;
        }
    }

    /**
     * Retention job: hard-delete notifications older than configured retention period (e.g. 90 days).
     * Runs daily to prevent unbounded database growth.
     */
    @Scheduled(cron = "${notification.retention-cron:0 0 3 * * ?}") // default: 3 AM daily
    @Transactional
    public void deleteNotificationsOlderThanRetentionPeriod() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteByCreatedAtBefore(cutoff);
        if (deleted > 0) {
            logger.info("Retention job: deleted {} notifications older than {} days (before {})", deleted, retentionDays, cutoff);
        }
    }

    private String getUserFcmToken(String userId) {
        String token = fcmTokenRepository.findTokenByUserId(userId);
        if (token == null) {
            throw new IllegalStateException("No FCM token found for user: " + userId);
        }
        return token;
    }

    /**
     * Determines whether a push should be delivered based on the recipient's preferences.
     * Returns true when push is globally enabled for the user AND the specific notification
     * type is enabled. If preferences cannot be resolved we fail open (deliver the push) so
     * that a preferences outage never silently suppresses transactional notifications.
     */
    private boolean isPushAllowed(NotificationEntity notification) {
        String userId = notification.getUserId();
        try {
            if (!preferencesService.isPushEnabled(userId)) {
                return false;
            }
            return notification.getType() == null
                    || preferencesService.isNotificationTypeEnabled(userId, notification.getType());
        } catch (Exception e) {
            logger.warn("Could not resolve notification preferences for user {}; defaulting to deliver. Cause: {}",
                    userId, e.getMessage());
            return true;
        }
    }
    
    @Recover
    private void recoverFromNotificationFailure(FirebaseMessagingException ex, NotificationEntity notification) {
        logger.error("All retry attempts failed for notification: {}", notification.getId(), ex);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setError("Max retry attempts exceeded: " + ex.getMessage());
        notification.setSentAt(OffsetDateTime.now());
        notificationRepository.save(notification);

        eventPublisher.publishNotificationFailed(
            new NotificationFailedEvent(
                notification.getId(),
                notification.getUserId(),
                "Max retry attempts exceeded: " + ex.getMessage(),
                notification.getSentAt()
            )
        );
    }

}
