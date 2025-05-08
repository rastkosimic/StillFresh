package com.stillfresh.app.paymentservice.model;


import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_users")
public class PaymentUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username; // 🔹 Store username as a unique identifier (extracted from JWT)

    @Column(nullable = false, unique = true)
    private String stripeCustomerId; // 🔹 Stripe customer ID

    public PaymentUser() {}

    public PaymentUser(String username, String stripeCustomerId) {
        this.username = username;
        this.stripeCustomerId = stripeCustomerId;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }
}
