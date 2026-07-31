package com.stillfresh.app.notificationservice.consumer;

import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.BankTransferConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Notifies the customer that their bank transfer has been received and the order is confirmed.
 * This closes the manual-transfer loop: the customer previously got the IBAN/reference
 * instructions (BankTransferInitiatedConsumer) and now gets a brief acknowledgement once an
 * admin confirms receipt of the funds.
 */
@Component
public class BankTransferConfirmedConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferConfirmedConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "${payment.topic.bank-transfer-confirmed:bank-transfer-confirmed}",
                   groupId = "notification-service")
    public void handleBankTransferConfirmed(BankTransferConfirmedEvent event) {
        try {
            logger.info("Received BankTransferConfirmedEvent: reference={}, userId={}",
                        event.getPaymentReference(), event.getUserId());

            Map<String, String> data = new HashMap<>();
            data.put("type", NotificationType.BANK_TRANSFER_CONFIRMED.name());
            data.put("orderId", String.valueOf(event.getOrderId()));
            if (event.getPaymentReference() != null) {
                data.put("reference", event.getPaymentReference());
            }

            NotificationRequestEvent notification = new NotificationRequestEvent(
                String.valueOf(event.getUserId()),
                NotificationType.BANK_TRANSFER_CONFIRMED,
                "Payment received",
                "We've received your payment. Your order is now confirmed.",
                data
            );

            notificationService.handleNotificationRequest(notification);

        } catch (Exception e) {
            logger.error("Failed to send bank transfer confirmation notification for reference={}: {}",
                         event.getPaymentReference(), e.getMessage(), e);
        }
    }
}
