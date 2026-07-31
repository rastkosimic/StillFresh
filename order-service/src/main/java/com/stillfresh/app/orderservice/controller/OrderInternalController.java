package com.stillfresh.app.orderservice.controller;

import com.stillfresh.app.orderservice.dto.OrderRatingEligibilityResponse;
import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/internal")
public class OrderInternalController {

    private static final Logger logger = LoggerFactory.getLogger(OrderInternalController.class);

    @Autowired
    private OrderService orderService;

    @GetMapping("/{orderId}/rating-eligibility")
    public ResponseEntity<OrderRatingEligibilityResponse> getRatingEligibility(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService) {

        if (internalService == null || internalService.isEmpty()) {
            logger.warn("Unauthorized internal service call attempt to /orders/internal/{}/rating-eligibility", orderId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return orderService.getOrderById(orderId)
                .map(this::toEligibilityResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private OrderRatingEligibilityResponse toEligibilityResponse(Order order) {
        boolean eligible = "COMPLETED".equals(order.getStatus());
        return new OrderRatingEligibilityResponse(
                order.getId(),
                order.getUserId(),
                order.getVendorId(),
                order.getStatus(),
                eligible
        );
    }
}
