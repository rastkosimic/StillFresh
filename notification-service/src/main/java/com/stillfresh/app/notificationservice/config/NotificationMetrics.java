package com.stillfresh.app.notificationservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public Counter notificationSentCounter() {
        return Counter.builder("notifications.sent")
                .description("Number of notifications sent")
                .register(meterRegistry);
    }
    
    public Counter notificationFailedCounter() {
        return Counter.builder("notifications.failed")
                .description("Number of notifications failed")
                .register(meterRegistry);
    }
    
    public Counter notificationReceivedCounter() {
        return Counter.builder("notifications.received")
                .description("Number of notifications received")
                .register(meterRegistry);
    }
    
    public Counter fcmTokenRegisteredCounter() {
        return Counter.builder("fcm.tokens.registered")
                .description("Number of FCM tokens registered")
                .register(meterRegistry);
    }
}
