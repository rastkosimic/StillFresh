package com.stillfresh.app.orderservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.sharedentities.payment.events.PaymentCapturedEvent;

/**
 * Legacy capture notification. Order status and financial snapshot are applied by
 * {@link OrderPaymentSettledListener} / {@link BankTransferConfirmedListener}.
 */
@Component
public class PaymentCapturedListener {
    private static final Logger logger = LoggerFactory.getLogger(PaymentCapturedListener.class);

    @KafkaListener(topics = "${payment.topic.payment-captured:payment-captured-topic}", groupId = "order-service-group")
    public void handlePaymentCapturedEvent(PaymentCapturedEvent event) {
        logger.debug("Received PaymentCapturedEvent for paymentIntentId={}, status={} (handled via OrderPaymentSettledEvent)",
                event.getPaymentIntentId(), event.getStatus());
    }
}
