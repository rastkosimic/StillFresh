package com.stillfresh.app.paymentservice.dto;

import java.io.Serializable;

public class StripeBankAccountDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String bankAccountId;
    private String accountHolderName;
    private String accountHolderType; // individual, company
    private String bankName;
    private String country;
    private String currency;
    private String last4; // Last 4 digits of account number
    private String routingNumber;
    private String status; // new, validated, verified, verification_failed, errored
    private Boolean defaultForCurrency;
    private String fingerprint;
    
    public StripeBankAccountDto() {
    }
    
    public String getBankAccountId() {
        return bankAccountId;
    }
    
    public void setBankAccountId(String bankAccountId) {
        this.bankAccountId = bankAccountId;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public String getAccountHolderType() {
        return accountHolderType;
    }
    
    public void setAccountHolderType(String accountHolderType) {
        this.accountHolderType = accountHolderType;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getLast4() {
        return last4;
    }
    
    public void setLast4(String last4) {
        this.last4 = last4;
    }
    
    public String getRoutingNumber() {
        return routingNumber;
    }
    
    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Boolean getDefaultForCurrency() {
        return defaultForCurrency;
    }
    
    public void setDefaultForCurrency(Boolean defaultForCurrency) {
        this.defaultForCurrency = defaultForCurrency;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}

