package com.stillfresh.app.orderservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.stillfresh.app.orderservice.service.OrderService;
import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;

@Component
public class OrderRequestListener {

    @Autowired
    private OrderService orderService;

    private static final Logger logger = LoggerFactory.getLogger(OrderRequestListener.class);

    @KafkaListener(topics = "${order.topic.order-request:order-request}", groupId = "order-service-group")
    public void handleOrderRequestEvent(OrderRequestEvent event) {
        logger.info("Received OrderRequestEvent for userId: {}, offerId: {}, quantity: {}", 
                     event.getUserId(), event.getOfferId(), event.getQuantity());
        try {
            // Validate and place the order
            orderService.handleOrderRequest(event);
        } catch (Exception e) {
            logger.error("Failed to process order for userId: {}, offerId: {}", 
                         event.getUserId(), event.getOfferId(), e);
        }
    }
}


