package com.stillfresh.app.vendorservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vendor_balance_transactions")
public class VendorBalanceTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long vendorId;
    
    @Column(nullable = false)
    private BigDecimal amount; // Positive for credits, negative for debits (in cents)
    
    @Column(nullable = false)
    private String currency;
    
    @Column(nullable = false)
    private String type; // ORDER_PAYMENT, PAYOUT, ADJUSTMENT, REFUND
    
    @Column(nullable = false)
    private String description;
    
    private Long orderId; // Reference to order if applicable
    
    private Long payoutId; // Reference to payout if applicable
    
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getVendorId() {
        return vendorId;
    }
    
    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public Long getPayoutId() {
        return payoutId;
    }
    
    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

