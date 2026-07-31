package com.stillfresh.app.vendorservice.model;

import com.stillfresh.app.sharedentities.enums.ManualPayoutMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vendor_payouts")
public class VendorPayout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long vendorId;
    
    @Column(nullable = false)
    private BigDecimal amount; // In cents
    
    @Column(nullable = false)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManualPayoutMethod method; // BANK, WISE, etc.
    
    @Column(nullable = false)
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    
    @Column(nullable = false)
    private OffsetDateTime requestedAt;
    
    private OffsetDateTime processedAt;
    
    private String transactionReference; // Bank transfer reference, etc.
    
    private String notes; // Admin notes
    
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
    
    public ManualPayoutMethod getMethod() {
        return method;
    }
    
    public void setMethod(ManualPayoutMethod method) {
        this.method = method;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}

