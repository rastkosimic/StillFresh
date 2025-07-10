package com.stillfresh.app.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;

@Component
public class NotificationRequestConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationRequestConsumer.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @KafkaListener(topics = "notification-request", groupId = "notification-service")
    public void consumeNotificationRequest(NotificationRequestEvent event) {
        try {
            logger.info("Received notification request for user: {}", event.getUserId());
            notificationService.handleNotificationRequest(event);
        } catch (Exception e) {
            logger.error("Failed to process notification request", e);
        }
    }
} 