package com.stillfresh.app.orderservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.orderservice.service.OrderService;
import com.stillfresh.app.sharedentities.payment.events.PaymentSuccessEvent;

@Component
public class PaymentSuccessListener {
	@Autowired
	private OrderService orderService;

	private static final Logger logger = LoggerFactory.getLogger(PaymentSuccessListener.class);

	@KafkaListener(topics = "${payment.topic.name:payment-success-topic}", groupId = "order-service-group")
	public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
		logger.info("Received PaymentSuccessEvent for userId: {}, offerId: {}, paymentIntentId: {}", 
				event.getUserId(), event.getOfferId(), event.getPaymentIntentId());
		try {
			// finalize the order with PaymentIntent ID for manual capture
			orderService.finalizeOrder(event.getRequestId(), event.getPaymentIntentId());
		} catch (Exception e) {
			logger.error("Failed to process order for userId: {}, offerId: {}", event.getUserId(), event.getOfferId(),
					e);
		}
	}
}
