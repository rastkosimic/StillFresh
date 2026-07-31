package com.stillfresh.app.paymentservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.paymentservice.provider.PaymentProviderRouter;
import com.stillfresh.app.paymentservice.service.BankTransferPaymentService;
import com.stillfresh.app.sharedentities.payment.events.PaymentCancelRequestEvent;

@Component
public class PaymentCancelRequestListener {
    
    @Autowired
    private PaymentProviderRouter paymentProviderRouter;

    @Autowired
    private BankTransferPaymentService bankTransferPaymentService;
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentCancelRequestListener.class);
    
    @KafkaListener(topics = "${payment.topic.payment-cancel-request:payment-cancel-request}", groupId = "payment-service-group")
    public void handlePaymentCancelRequest(PaymentCancelRequestEvent event) {
        logger.info("Received PaymentCancelRequestEvent for paymentIntentId: {}, orderId: {}, userId: {}", 
                event.getPaymentIntentId(), event.getOrderId(), event.getUserId());
        try {
            if (event.getPaymentIntentId() != null && !event.getPaymentIntentId().isBlank()) {
                // Release the hold via the active provider (Stripe PaymentIntent cancel or AllSecure void)
                paymentProviderRouter.active().cancel(event.getPaymentIntentId());
                logger.info("Cancellation handled for reference {} (order {})",
                        event.getPaymentIntentId(), event.getOrderId());
            } else if (event.getOrderId() != null) {
                // Bank transfer: cancel by orderId (no payment hold involved)
                bankTransferPaymentService.cancelByOrderId(event.getOrderId());
                logger.info("Handled bank transfer cancellation for orderId={}", event.getOrderId());
            } else {
                logger.warn("PaymentCancelRequestEvent has neither paymentIntentId nor orderId. Skipping.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error cancelling payment for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}

