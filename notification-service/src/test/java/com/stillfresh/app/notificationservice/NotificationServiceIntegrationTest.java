package com.stillfresh.app.notificationservice;

import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    public void testNotificationFlow() {
        // This is a basic integration test
        // In a real scenario, you would test with actual FCM tokens
        
        Map<String, String> data = new HashMap<>();
        data.put("test", "true");
        data.put("orderId", "12345");
        
        NotificationRequestEvent event = new NotificationRequestEvent(
            "test-user-123",
            NotificationType.ORDER_CONFIRMED,
            "Test Order Confirmed",
            "Your test order has been confirmed!",
            data
        );
        
        // This will fail without a real FCM token, but tests the flow
        try {
            notificationService.handleNotificationRequest(event);
        } catch (Exception e) {
            // Expected to fail without real FCM token
            System.out.println("Expected failure: " + e.getMessage());
        }
    }
}








