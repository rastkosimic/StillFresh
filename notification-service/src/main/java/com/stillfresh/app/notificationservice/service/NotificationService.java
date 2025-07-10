package com.stillfresh.app.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

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
            sendPushNotification(notification);

        } catch (Exception e) {
            logger.error("Failed to process notification request", e);
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

    @Transactional
    private void sendPushNotification(NotificationEntity notification) {
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

            eventPublisher.publishNotificationSent(
                new NotificationSentEvent(
                    notification.getId(),
                    notification.getUserId(),
                    notification.getType(),
                    notification.getSentAt()
                )
            );

        } catch (FirebaseMessagingException e) {
            logger.error("Firebase error while sending notification", e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setError(e.getMessage());
            notification.setSentAt(OffsetDateTime.now());
            notificationRepository.save(notification);

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
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String getUserFcmToken(String userId) {
        String token = fcmTokenRepository.findTokenByUserId(userId);
        if (token == null) {
            throw new IllegalStateException("No FCM token found for user: " + userId);
        }
        return token;
    }

}
