package com.stillfresh.app.userservice.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.stillfresh.app.sharedentities.order.events.OrderRequestEvent;
import com.stillfresh.app.userservice.service.UserService;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private UserService userService;

    @PostMapping("/place-order")
    public ResponseEntity<?> placeOrder(Principal principal, @RequestBody OrderRequestEvent orderRequest) {
        try {
            if (orderRequest.getRequestId() == null || orderRequest.getRequestId().isBlank()) {
                orderRequest.setRequestId(UUID.randomUUID().toString());
            }
            userService.publishOrderRequest(principal, orderRequest);
            return ResponseEntity.ok(Map.of(
                    "message", "Order request submitted successfully.",
                    "requestId", orderRequest.getRequestId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to place order: " + e.getMessage());
        }
    }
}

