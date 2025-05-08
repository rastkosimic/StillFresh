package com.stillfresh.app.paymentservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.paymentservice.service.PaymentService;
import com.stillfresh.app.sharedentities.payment.events.UpdatePaymentServiceEvent;

@Component
public class PaymentServiceUpdateListener {
	@Autowired
	PaymentService paymentService;
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentServiceUpdateListener.class);
  
	@KafkaListener(topics = "${payment.topic.name:payment-service-update}", groupId = "payment-service-group")
	public void handlePaymentServiceUpdateEvent(UpdatePaymentServiceEvent event) {
		logger.info("Received UpdatePaymentServiceEvent for old username: {}", event.getOldUsername());
		try {
			paymentService.processPaymentServiceUpdate(event);
		} catch (Exception e) {
			logger.error("Failed to process UpdatePaymentServiceEvent for old username: {}", event.getOldUsername(), e);
		}
	}

}
