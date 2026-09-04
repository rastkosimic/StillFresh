package com.stillfresh.app.orderservice.controller;

import com.stillfresh.app.orderservice.dto.OrderRatingEligibilityResponse;
import com.stillfresh.app.orderservice.model.Order;
import com.stillfresh.app.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/internal")
public class OrderInternalController {

    @Autowired
    private OrderService orderService;

    // Authorization is enforced by InternalServiceFilter and WebSecurityConfig, which require
    // the shared internal secret for /orders/internal/**. The previous check here only tested
    // that an X-Internal-Service header was non-empty, which any caller could satisfy.
    @GetMapping("/{orderId}/rating-eligibility")
    public ResponseEntity<OrderRatingEligibilityResponse> getRatingEligibility(@PathVariable Long orderId) {
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
