package com.stillfresh.app.sharedentities.notification.events;

import java.util.Map;
import java.util.UUID;

import com.stillfresh.app.sharedentities.enums.NotificationType;

public class NotificationRequestEvent {
    private UUID notificationId;
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, String> data;

    public NotificationRequestEvent() {
    }

    public NotificationRequestEvent(String userId, NotificationType type, String title, String message, Map<String, String> data) {
        this.notificationId = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.data = data;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
} 