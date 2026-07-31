package com.stillfresh.app.paymentservice.listener;

import com.stillfresh.app.paymentservice.service.PaymentService;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for vendor payment info responses from vendor-service
 */
@Component
public class VendorPaymentInfoResponseListener {

    private static final Logger logger = LoggerFactory.getLogger(VendorPaymentInfoResponseListener.class);

    @Autowired
    private PaymentService paymentService;

    @KafkaListener(topics = "${payment.topic.vendor-payment-info-response:vendor-payment-info-response}", groupId = "payment-service-group")
    public void handleVendorPaymentInfoResponse(VendorPaymentInfoResponseEvent event) {
        logger.info("Received VendorPaymentInfoResponseEvent for request ID: {}, vendorId: {}, success: {}", 
                   event.getRequestId(), event.getVendorId(), event.isSuccess());
        try {
            paymentService.handleVendorPaymentInfoResponse(event);
        } catch (Exception e) {
            logger.error("Error handling vendor payment info response: {}", e.getMessage(), e);
        }
    }
}

