package com.stillfresh.app.paymentservice.dto;

import java.io.Serializable;
import java.time.Instant;

public class StripeTransactionDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String transactionId;
    private Long amount; // Amount in cents
    private String currency;
    private String description;
    private Instant created;
    private String type; // charge, payment, payout, refund, transfer, etc.
    private String status; // available, pending
    private Long fee; // Fee amount in cents
    private Long net; // Net amount in cents (after fees)
    private String source; // Source transaction ID
    private String reportingCategory; // advance, advance_funding, charge, charge_failure, etc.
    
    public StripeTransactionDto() {
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Instant getCreated() {
        return created;
    }
    
    public void setCreated(Instant created) {
        this.created = created;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getFee() {
        return fee;
    }
    
    public void setFee(Long fee) {
        this.fee = fee;
    }
    
    public Long getNet() {
        return net;
    }
    
    public void setNet(Long net) {
        this.net = net;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getReportingCategory() {
        return reportingCategory;
    }
    
    public void setReportingCategory(String reportingCategory) {
        this.reportingCategory = reportingCategory;
    }
}

