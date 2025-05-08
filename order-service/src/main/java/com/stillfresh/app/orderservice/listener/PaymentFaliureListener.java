package com.stillfresh.app.orderservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.orderservice.service.OrderService;
import com.stillfresh.app.sharedentities.payment.events.PaymentFailureEvent;

@Component
public class PaymentFaliureListener {
	@Autowired
	private OrderService orderService;

	private static final Logger logger = LoggerFactory.getLogger(PaymentFaliureListener.class);

	@KafkaListener(topics = "${payment.topic.name:payment-faliure-topic}", groupId = "order-service-group")
	public void handlePaymentFaliureEvent(PaymentFailureEvent event) {
		logger.info("Received PaymentFailureEvent for userId: {}, offerId: {}", event.getUserId(), event.getOfferId());
		try {
			// finalize the order
			orderService.cancelOrder(event.getRequestId());
		} catch (Exception e) {
			logger.error("Failed to process order for userId: {}, offerId: {}", event.getUserId(), event.getOfferId(),
					e);
		}
	}
}
