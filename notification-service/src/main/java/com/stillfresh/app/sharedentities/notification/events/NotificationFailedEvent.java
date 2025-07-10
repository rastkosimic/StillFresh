package com.stillfresh.app.sharedentities.notification.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationFailedEvent {
    private UUID notificationId;
    private String userId;
    private String errorMessage;
    private OffsetDateTime failedAt;

    public NotificationFailedEvent() {
    }

    public NotificationFailedEvent(UUID notificationId, String userId, String errorMessage, OffsetDateTime failedAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.errorMessage = errorMessage;
        this.failedAt = failedAt;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(OffsetDateTime failedAt) {
        this.failedAt = failedAt;
    }
}
