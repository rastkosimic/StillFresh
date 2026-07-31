package com.stillfresh.app.notificationservice.consumer;

import com.stillfresh.app.notificationservice.service.EmailService;
import com.stillfresh.app.notificationservice.service.NotificationService;
import com.stillfresh.app.notificationservice.service.UserContactService;
import com.stillfresh.app.sharedentities.enums.NotificationType;
import com.stillfresh.app.sharedentities.notification.events.NotificationRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.BankTransferInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles bank transfer payment instructions after a bank-transfer order is placed.
 * The full instructions (IBAN, reference, amount, deadline) are delivered by email - the
 * channel where the customer can copy/paste the details into their banking app. The push
 * is reduced to a short nudge pointing the customer to the email.
 */
@Component
public class BankTransferInitiatedConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferInitiatedConsumer.class);
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Belgrade"));

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserContactService userContactService;

    @KafkaListener(topics = "${payment.topic.bank-transfer-initiated:bank-transfer-initiated}",
                   groupId = "notification-service")
    public void handleBankTransferInitiated(BankTransferInitiatedEvent event) {
        try {
            logger.info("Received BankTransferInitiatedEvent: reference={}, userId={}",
                        event.getPaymentReference(), event.getUserId());

            double amountInUnits = event.getAmountCents() / 100.0;
            String deadline = event.getExpiresAt() != null ? DT_FMT.format(event.getExpiresAt()) : "N/A";

            // Email carries the full, copy-paste-friendly instructions.
            sendInstructionsEmail(event, amountInUnits, deadline);

            // Push is a short nudge; keep the data map for deep-linking into the app.
            Map<String, String> data = new HashMap<>();
            data.put("type",           NotificationType.BANK_TRANSFER_INITIATED.name());
            data.put("reference",      event.getPaymentReference());
            data.put("iban",           event.getIban());
            data.put("bank_name",      event.getBankName());
            data.put("account_holder", event.getAccountHolder());
            data.put("amount",         String.format("%.2f", amountInUnits));
            data.put("currency",       event.getCurrency());
            data.put("description",    event.getPaymentDescription());
            data.put("expires_at",     deadline);
            data.put("orderId",        String.valueOf(event.getOrderId()));

            NotificationRequestEvent notification = new NotificationRequestEvent(
                String.valueOf(event.getUserId()),
                NotificationType.BANK_TRANSFER_INITIATED,
                "Complete your payment",
                "Check your email for the bank transfer instructions to complete your payment.",
                data
            );

            notificationService.handleNotificationRequest(notification);

        } catch (Exception e) {
            logger.error("Failed to send bank transfer notification for reference={}: {}",
                         event.getPaymentReference(), e.getMessage(), e);
        }
    }

    private void sendInstructionsEmail(BankTransferInitiatedEvent event, double amountInUnits, String deadline) {
        if (!emailService.isEnabled()) {
            return;
        }
        String to = userContactService.resolveEmail(String.valueOf(event.getUserId()));
        if (to == null || to.isBlank()) {
            logger.warn("No email resolved for user {}; skipping bank transfer instructions email", event.getUserId());
            return;
        }
        String subject = "StillFresh - Complete your payment";
        String body = String.format(
            "Hello,%n%n" +
            "To complete your order, please make a bank transfer with the following details:%n%n" +
            "Amount:          %.2f %s%n" +
            "Account holder:  %s%n" +
            "IBAN:            %s%n" +
            "Bank:            %s%n" +
            "Reference:       %s%n" +
            "Pay by:          %s%n%n" +
            "%s%n%n" +
            "Your order will be confirmed once we receive your payment.%n%n" +
            "Thank you,%nThe StillFresh team",
            amountInUnits, event.getCurrency(),
            event.getAccountHolder(),
            event.getIban(),
            event.getBankName(),
            event.getPaymentReference(),
            deadline,
            event.getPaymentDescription() != null ? event.getPaymentDescription() : ""
        );
        emailService.sendEmail(to, subject, body);
    }
}
