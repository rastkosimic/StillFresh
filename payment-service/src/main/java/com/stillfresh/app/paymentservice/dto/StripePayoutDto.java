package com.stillfresh.app.paymentservice.dto;

import java.io.Serializable;
import java.time.Instant;

public class StripePayoutDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String payoutId;
    private Long amount; // Amount in cents
    private String currency;
    private String status; // paid, pending, in_transit, canceled, failed
    private Instant arrivalDate;
    private Instant created;
    private String description;
    private String destination; // Bank account ID
    private String failureCode;
    private String failureMessage;
    private String method; // standard, instant
    private String statementDescriptor;
    
    public StripePayoutDto() {
    }
    
    public String getPayoutId() {
        return payoutId;
    }
    
    public void setPayoutId(String payoutId) {
        this.payoutId = payoutId;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Instant getArrivalDate() {
        return arrivalDate;
    }
    
    public void setArrivalDate(Instant arrivalDate) {
        this.arrivalDate = arrivalDate;
    }
    
    public Instant getCreated() {
        return created;
    }
    
    public void setCreated(Instant created) {
        this.created = created;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getFailureCode() {
        return failureCode;
    }
    
    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }
    
    public String getFailureMessage() {
        return failureMessage;
    }
    
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getStatementDescriptor() {
        return statementDescriptor;
    }
    
    public void setStatementDescriptor(String statementDescriptor) {
        this.statementDescriptor = statementDescriptor;
    }
}

