package com.stillfresh.app.notificationservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.notificationservice.service.EmailService;
import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.notificationservice.service.UserContactService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentSuccessEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentFailureEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentNotificationConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationConsumer.class);
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserContactService userContactService;
    
    @KafkaListener(topics = "${payment.topic.payment-success:payment-success}", 
                   groupId = "notification-service")
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        try {
            logger.info("Received payment success event for user: {}, offer: {}", 
                       event.getUserId(), event.getOfferId());
            
            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.PAYMENT_SUCCESSFUL.name());
            data.put("requestId", event.getRequestId());
            data.put("offerId", String.valueOf(event.getOfferId()));
            
            NotificationRequestEvent notificationRequest = new NotificationRequestEvent(
                event.getUserId().toString(),
                NotificationType.PAYMENT_SUCCESSFUL,
                "Payment Successful",
                "Your payment has been processed successfully!",
                data
            );
            
            notificationService.handleNotificationRequest(notificationRequest);

            // Email receipt for the payment (recipient resolved from userId).
            sendPaymentReceiptEmail(event);
            
        } catch (Exception e) {
            logger.error("Failed to process payment success event", e);
        }
    }

    private void sendPaymentReceiptEmail(PaymentSuccessEvent event) {
        if (!emailService.isEnabled()) {
            return;
        }
        String to = userContactService.resolveEmail(event.getUserId().toString());
        if (to == null || to.isBlank()) {
            logger.warn("No email resolved for user {}; skipping payment receipt email", event.getUserId());
            return;
        }
        String body = String.format(
            "Hello,%n%n" +
            "Your payment has been processed successfully.%n%n" +
            "Order reference: %s%n%n" +
            "You can view the details of your order in the StillFresh app.%n%n" +
            "Thank you,%nThe StillFresh team",
            event.getRequestId()
        );
        emailService.sendEmail(to, "StillFresh - Payment Receipt", body);
    }
    
    @KafkaListener(topics = "${payment.topic.payment-failure:payment-failure}", 
                   groupId = "notification-service")
    public void handlePaymentFailureEvent(PaymentFailureEvent event) {
        try {
            logger.info("Received payment failure event for user: {}, offer: {}", 
                       event.getUserId(), event.getOfferId());
            
            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.PAYMENT_FAILED.name());
            data.put("requestId", event.getRequestId());
            data.put("offerId", String.valueOf(event.getOfferId()));
            data.put("reason", event.getFailureReason());
            
            // Create a more informative and user-friendly error message
            String errorMessage = buildErrorMessage(event.getFailureReason());
            
            NotificationRequestEvent notificationRequest = new NotificationRequestEvent(
                event.getUserId().toString(),
                NotificationType.PAYMENT_FAILED,
                "Payment Rejected",
                errorMessage,
                data
            );
            
            notificationService.handleNotificationRequest(notificationRequest);
            
        } catch (Exception e) {
            logger.error("Failed to process payment failure event", e);
        }
    }
    
    /**
     * Builds a user-friendly error message based on the failure reason
     */
    private String buildErrorMessage(String failureReason) {
        if (failureReason == null || failureReason.isEmpty()) {
            return "Your payment could not be processed. Please check your payment method and try again.";
        }
        
        String lowerReason = failureReason.toLowerCase();
        
        // Map common Stripe error messages to user-friendly text
        if (lowerReason.contains("insufficient funds") || lowerReason.contains("decline")) {
            return "Your payment was declined. Please check your card details or try a different payment method.";
        } else if (lowerReason.contains("expired")) {
            return "Your card has expired. Please update your payment method.";
        } else if (lowerReason.contains("invalid") || lowerReason.contains("incorrect")) {
            return "Invalid payment information. Please verify your card details.";
        } else if (lowerReason.contains("timeout")) {
            return "Payment timed out. Please try again.";
        } else {
            return "Your payment could not be processed: " + failureReason + ". Please try again or contact support.";
        }
    }
}








