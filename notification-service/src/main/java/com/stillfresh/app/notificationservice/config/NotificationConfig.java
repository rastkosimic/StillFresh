package com.stillfresh.app.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableRetry
@EnableScheduling
public class NotificationConfig {
    
    @Bean
    public NotificationMetrics notificationMetrics() {
        return new NotificationMetrics();
    }
}








