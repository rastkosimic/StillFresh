package com.stillfresh.app.orderservice.listener;

import com.stillfresh.app.orderservice.repository.OrderRepository;
import com.stillfresh.app.orderservice.service.OrderSettlementService;
import com.stillfresh.app.sharedentities.payment.events.BankTransferConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BankTransferConfirmedListener {

    private static final Logger logger = LoggerFactory.getLogger(BankTransferConfirmedListener.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSettlementService orderSettlementService;

    @KafkaListener(topics = "${payment.topic.bank-transfer-confirmed:bank-transfer-confirmed}", groupId = "order-service-group")
    public void handleBankTransferConfirmed(BankTransferConfirmedEvent event) {
        logger.info("Received BankTransferConfirmedEvent for orderId={}", event.getOrderId());

        try {
            orderSettlementService.applySettlementSnapshot(
                    orderRepository.findById(event.getOrderId()),
                    event.getGrossAmountCents(),
                    event.getPlatformFeeCents(),
                    event.getNetAmountCents(),
                    event.getFeePercentApplied(),
                    event.getCurrency()
            );
        } catch (Exception e) {
            logger.error("Failed to apply bank transfer settlement for orderId={}", event.getOrderId(), e);
        }
    }
}
