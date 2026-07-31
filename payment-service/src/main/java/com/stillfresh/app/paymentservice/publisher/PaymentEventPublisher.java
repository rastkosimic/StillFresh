package com.stillfresh.app.paymentservice.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.stillfresh.app.sharedentities.payment.events.MoRPayoutDataRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.MoRPayoutStatusUpdateRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoRequestEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentFailureEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentSuccessEvent;
import com.stillfresh.app.sharedentities.payment.events.PaymentCapturedEvent;
import com.stillfresh.app.sharedentities.payment.events.OrderPaymentSettledEvent;
import com.stillfresh.app.sharedentities.payment.events.BankTransferInitiatedEvent;
import com.stillfresh.app.sharedentities.payment.events.BankTransferConfirmedEvent;

@Service
public class PaymentEventPublisher {

	private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Value("${payment.topic.payment-success:payment-success-topic}")
	private String paymentSuccessTopic;

	@Value("${payment.topic.payment-failure:payment-failure-topic}")
	private String paymentFailureTopic;

	@Value("${payment.topic.mor-payout-data-request:mor-payout-data-request}")
	private String morPayoutDataRequestTopic;

	@Value("${payment.topic.mor-payout-status-update-request:mor-payout-status-update-request}")
	private String morPayoutStatusUpdateRequestTopic;

	@Value("${payment.topic.vendor-payment-info-request:vendor-payment-info-request}")
	private String vendorPaymentInfoRequestTopic;

	@Value("${payment.topic.payment-captured:payment-captured-topic}")
	private String paymentCapturedTopic;

	@Value("${payment.topic.order-payment-settled:order-payment-settled}")
	private String orderPaymentSettledTopic;

	@Value("${payment.topic.bank-transfer-initiated:bank-transfer-initiated}")
	private String bankTransferInitiatedTopic;

	@Value("${payment.topic.bank-transfer-confirmed:bank-transfer-confirmed}")
	private String bankTransferConfirmedTopic;

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
			logger.info("Published PaymentFailureEvent to Kafka topic '{}'", paymentFailureTopic);
			kafkaTemplate.send(paymentFailureTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish PaymentFailureEvent to Kafka", e);
		}
	}

	public void publishMoRPayoutDataRequest(MoRPayoutDataRequestEvent event) {
		try {
			logger.info("Published MoRPayoutDataRequestEvent to Kafka topic '{}'", morPayoutDataRequestTopic);
			kafkaTemplate.send(morPayoutDataRequestTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish MoRPayoutDataRequestEvent to Kafka", e);
		}
	}

	public void publishMoRPayoutStatusUpdateRequest(MoRPayoutStatusUpdateRequestEvent event) {
		try {
			logger.info("Published MoRPayoutStatusUpdateRequestEvent to Kafka topic '{}'", morPayoutStatusUpdateRequestTopic);
			kafkaTemplate.send(morPayoutStatusUpdateRequestTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish MoRPayoutStatusUpdateRequestEvent to Kafka", e);
		}
	}

	public void publishVendorPaymentInfoRequest(VendorPaymentInfoRequestEvent event) {
		try {
			logger.info("Published VendorPaymentInfoRequestEvent to Kafka topic '{}': requestId={}, vendorId={}", 
			           vendorPaymentInfoRequestTopic, event.getRequestId(), event.getVendorId());
			kafkaTemplate.send(vendorPaymentInfoRequestTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish VendorPaymentInfoRequestEvent to Kafka", e);
		}
	}

	public void publishPaymentCapturedEvent(PaymentCapturedEvent event) {
		try {
			logger.info("Published PaymentCapturedEvent to Kafka topic '{}': paymentIntentId={}, status={}",
			           paymentCapturedTopic, event.getPaymentIntentId(), event.getStatus());
			kafkaTemplate.send(paymentCapturedTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish PaymentCapturedEvent to Kafka", e);
		}
	}

	public void publishBankTransferInitiatedEvent(BankTransferInitiatedEvent event) {
		try {
			logger.info("Published BankTransferInitiatedEvent to topic '{}': reference={}, orderId={}",
			           bankTransferInitiatedTopic, event.getPaymentReference(), event.getOrderId());
			kafkaTemplate.send(bankTransferInitiatedTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish BankTransferInitiatedEvent to Kafka", e);
		}
	}

	public void publishBankTransferConfirmedEvent(BankTransferConfirmedEvent event) {
		try {
			logger.info("Published BankTransferConfirmedEvent to topic '{}': reference={}, orderId={}",
			           bankTransferConfirmedTopic, event.getPaymentReference(), event.getOrderId());
			kafkaTemplate.send(bankTransferConfirmedTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish BankTransferConfirmedEvent to Kafka", e);
		}
	}

	public void publishOrderPaymentSettledEvent(OrderPaymentSettledEvent event) {
		try {
			logger.info("Published OrderPaymentSettledEvent to Kafka topic '{}': paymentIntentId={}, vendorId={}, net={} {}",
			           orderPaymentSettledTopic, event.getPaymentIntentId(), event.getVendorId(),
			           event.getNetAmountCents(), event.getCurrency());
			kafkaTemplate.send(orderPaymentSettledTopic, event);
		} catch (Exception e) {
			logger.error("Failed to publish OrderPaymentSettledEvent to Kafka", e);
		}
	}
}
