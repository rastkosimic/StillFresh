package com.stillfresh.app.sharedentities.notification.events;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.stillfresh.app.sharedentities.enums.NotificationType;

public class NotificationSentEvent {
    private UUID notificationId;
    private String userId;
    private NotificationType type;
    private OffsetDateTime sentAt;

    public NotificationSentEvent() {
    }

    public NotificationSentEvent(UUID notificationId, String userId, NotificationType type, OffsetDateTime sentAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.sentAt = sentAt;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
