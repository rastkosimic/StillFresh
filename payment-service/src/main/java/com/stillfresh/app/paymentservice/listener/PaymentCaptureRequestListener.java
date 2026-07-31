package com.stillfresh.app.paymentservice.listener;

import com.stillfresh.app.paymentservice.provider.PaymentProviderRouter;
import com.stillfresh.app.sharedentities.payment.events.PaymentCaptureRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCaptureRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCaptureRequestListener.class);

    @Autowired
    private PaymentProviderRouter paymentProviderRouter;

    @KafkaListener(
            topics = "${payment.topic.payment-capture-request:payment-capture-request}",
            groupId = "payment-service-group")
    public void handlePaymentCaptureRequest(PaymentCaptureRequestEvent event) {
        logger.info("Received PaymentCaptureRequestEvent for paymentIntentId={}, orderId={}, userId={}",
                event.getPaymentIntentId(), event.getOrderId(), event.getUserId());
        if (event.getPaymentIntentId() == null || event.getPaymentIntentId().isBlank()) {
            logger.warn("PaymentCaptureRequestEvent missing paymentIntentId; skipping.");
            return;
        }
        try {
            // Capture via the active provider (Stripe PaymentIntent or AllSecure preauth reference)
            paymentProviderRouter.active().capture(event.getPaymentIntentId());
            logger.info("Capture handled for reference {} (order {})",
                    event.getPaymentIntentId(), event.getOrderId());
        } catch (Exception e) {
            logger.error("Unexpected error capturing payment for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
