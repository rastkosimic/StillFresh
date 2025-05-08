package com.stillfresh.app.paymentservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.payment.events.PaymentFailureEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentSuccessEvent;
import com.stillfresh.app.sharedentities.shared.events.TokenRequestEvent;

@Service
public class PaymentEventPublisher {

	private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Value("${authorization.topic.name:token-validation-request}")
	private String tokenVaidationRequestTopic;

	@Value("${payment.topic.name:payment-success-topic}")
	private String paymentSuccessTopic;

	@Value("${payment.topic.name:payment-faliure-topic}")
	private String paymentFaliureTopic;

	public void publishTokenValidationRequest(TokenRequestEvent event) {
		try {
			logger.info("Published TokenValidationEvent to Kafka topic '{}'", tokenVaidationRequestTopic);
			kafkaTemplate.send(tokenVaidationRequestTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish TokenValidationEvent to Kafka", e);
		}
	}

	public void publishPaymentSuccessEvent(PaymentSuccessEvent event) {
		try {
			logger.info("Published PaymentSuccessEvent to Kafka topic '{}'", paymentSuccessTopic);
			kafkaTemplate.send(paymentSuccessTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish PaymentSuccessEvent to Kafka", e);
		}

	}

	public void publishPaymentFailureEvent(PaymentFailureEvent event) {
		try {
			logger.info("Published PaymentFailureEvent to Kafka topic '{}'", paymentFaliureTopic);
			kafkaTemplate.send(paymentFaliureTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish PaymentFailureEvent to Kafka", e);
		}
	}
}
