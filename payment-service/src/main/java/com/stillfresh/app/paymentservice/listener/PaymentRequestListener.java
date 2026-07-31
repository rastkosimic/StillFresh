package com.stillfresh.app.paymentservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.paymentservice.provider.PaymentProviderRouter;
import com.stillfresh.app.sharedentities.payment.events.PaymentRequestEvent;

@Component
public class PaymentRequestListener {
	@Autowired
	private PaymentProviderRouter paymentProviderRouter;
	
  private static final Logger logger = LoggerFactory.getLogger(PaymentRequestListener.class);
  
	@KafkaListener(topics = "${payment.topic.payment-request:payment-request}", groupId = "payment-service-group")
	public void handlePaymentRequestEvent(PaymentRequestEvent event) {
		logger.info("Received PaymentRequestEvent for userId: {}, offerId: {}, amount: {}, requestId: {}",
				event.getUserId(), event.getOfferId(), event.getAmount(), event.getRequestId());
		try {
			// Authorize the payment via the active provider (Stripe or AllSecure)
			paymentProviderRouter.active().preauthorize(event);
		} catch (Exception e) {
			logger.error("Failed to process payment for userId: {}, offerId: {}", event.getUserId(), event.getOfferId(),
					e);
		}

	}
}
