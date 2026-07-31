package com.stillfresh.app.orderservice.service;

import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class OrderSettlementService {

    private static final Logger logger = LoggerFactory.getLogger(OrderSettlementService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public void applySettlementSnapshot(
            Optional<Order> orderOpt,
            Long grossAmountCents,
            Long platformFeeCents,
            Long netAmountCents,
            Double feePercentApplied,
            String currency) {

        if (orderOpt.isEmpty()) {
            return;
        }

        Order order = orderOpt.get();

        if ("CANCELLED".equals(order.getStatus())) {
            logger.warn("Order {} is CANCELLED; settlement snapshot skipped", order.getId());
            return;
        }

        order.setGrossAmountCents(grossAmountCents);
        order.setPlatformFeeCents(platformFeeCents);
        order.setNetAmountCents(netAmountCents);
        order.setFeePercentApplied(feePercentApplied);
        if (currency != null && !currency.isBlank()) {
            order.setCurrency(currency);
        }
        order.setSettledAt(OffsetDateTime.now());
        order.setStatus("COMPLETED");
        orderRepository.save(order);

        logger.info("Order {} settlement snapshot applied: gross={}, fee={}, net={}, feePercent={}",
                order.getId(), grossAmountCents, platformFeeCents, netAmountCents, feePercentApplied);
    }
}
