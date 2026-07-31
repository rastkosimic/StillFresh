package com.stillfresh.app.orderservice.listener;

import com.stillfresh.app.orderservice.repository.OrderRepository;
import com.stillfresh.app.orderservice.service.OrderSettlementService;
import com.stillfresh.app.sharedentities.payment.events.OrderPaymentSettledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentSettledListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderPaymentSettledListener.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSettlementService orderSettlementService;

    @KafkaListener(topics = "${payment.topic.order-payment-settled:order-payment-settled}", groupId = "order-service-group")
    public void handleOrderPaymentSettled(OrderPaymentSettledEvent event) {
        logger.info("Received OrderPaymentSettledEvent for paymentIntentId={}", event.getPaymentIntentId());

        try {
            orderSettlementService.applySettlementSnapshot(
                    orderRepository.findByPaymentIntentId(event.getPaymentIntentId()),
                    event.getGrossAmountCents(),
                    event.getPlatformFeeCents(),
                    event.getNetAmountCents(),
                    event.getFeePercentApplied(),
                    event.getCurrency()
            );
        } catch (Exception e) {
            logger.error("Failed to apply settlement snapshot for paymentIntentId={}",
                    event.getPaymentIntentId(), e);
        }
    }
}
