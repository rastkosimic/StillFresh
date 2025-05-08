package com.stillfresh.app.paymentservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.paymentservice.service.PaymentService;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;

@Component
public class PaymentRequestListener {
	@Autowired
	PaymentService paymentService;
	
  private static final Logger logger = LoggerFactory.getLogger(PaymentRequestListener.class);
  
	@KafkaListener(topics = "${payment.topic.payment-request:payment-request}", groupId = "payment-service-group")
	public void handlePaymentRequestEvent(PaymentRequestEvent event) {
		logger.info("Received PaymentRequestEvent for userId: {}, offerId: {}, amount: {}, requestId: {}",
				event.getUserId(), event.getOfferId(), event.getAmount(), event.getRequestId());
		try {
			// Validate and pay the order
			paymentService.processPaymentRequest(event);
		} catch (Exception e) {
			logger.error("Failed to process payment for userId: {}, offerId: {}", event.getUserId(), event.getOfferId(),
					e);
		}

	}
}
