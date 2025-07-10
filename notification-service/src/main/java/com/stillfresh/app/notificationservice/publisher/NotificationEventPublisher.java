package com.stillfresh.app.notificationservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.notification.events.NotificationFailedEvent;
import com.stillfresh.app.sharedentities.notification.events.NotificationSentEvent;

@Component
public class NotificationEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishNotificationSent(NotificationSentEvent event) {
        try {
            kafkaTemplate.send("notification-sent", event.getUserId(), event);
            logger.info("Published notification sent event for user: {}", event.getUserId());
        } catch (Exception e) {
            logger.error("Failed to publish notification sent event", e);
        }
    }
    
    public void publishNotificationFailed(NotificationFailedEvent event) {
        try {
            kafkaTemplate.send("notification-failed", event.getUserId(), event);
            logger.info("Published notification failed event for user: {}", event.getUserId());
        } catch (Exception e) {
            logger.error("Failed to publish notification failed event", e);
        }
    }
} 