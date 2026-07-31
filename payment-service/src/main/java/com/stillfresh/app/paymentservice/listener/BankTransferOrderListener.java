package com.stillfresh.app.paymentservice.listener;

import com.stillfresh.app.paymentservice.service.BankTransferPaymentService;
import com.stillfresh.app.sharedentities.order.events.BankTransferOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BankTransferOrderListener {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferOrderListener.class);

    @Autowired
    private BankTransferPaymentService bankTransferPaymentService;

    @KafkaListener(topics = "${payment.topic.bank-transfer-order:bank-transfer-order}",
                   groupId = "payment-service-group")
    public void handleBankTransferOrder(BankTransferOrderEvent event) {
        logger.info("Received BankTransferOrderEvent: orderId={}, userId={}, amount={} {}",
                    event.getOrderId(), event.getUserId(), event.getGrossAmountCents(), event.getCurrency());
        try {
            bankTransferPaymentService.initiate(event);
        } catch (Exception e) {
            logger.error("Failed to initiate bank transfer for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}
