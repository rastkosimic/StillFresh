package com.stillfresh.app.paymentservice.controller;

import com.stillfresh.app.paymentservice.dto.CardRegistrationRequest;
import com.stillfresh.app.paymentservice.dto.CardRegistrationResponse;
import com.stillfresh.app.paymentservice.dto.PaymentRequest;
import com.stillfresh.app.paymentservice.dto.PaymentResponse;
import com.stillfresh.app.paymentservice.service.PaymentService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    
    //Register a Card
    @PostMapping("/register-card")
    public ResponseEntity<CardRegistrationResponse> registerCard(@RequestBody CardRegistrationRequest request, Principal principal) {
        CardRegistrationResponse response = paymentService.registerCard(request, principal);
        return ResponseEntity.ok(response);
    }

    //Make a One-Time Payment
    @PostMapping("/charge")
    public ResponseEntity<PaymentResponse> charge(@RequestBody PaymentRequest request, Principal principal) {
        PaymentResponse response = paymentService.charge(request, principal);
        return ResponseEntity.ok(response);
    }

}
