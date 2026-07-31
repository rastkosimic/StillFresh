package com.stillfresh.app.paymentservice.dto;

import java.io.Serializable;
import java.util.Map;

public class StripeAccountDetailsDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String accountId;
    private String email;
    private String country;
    private String defaultCurrency;
    private String type; // express, standard, custom
    private Boolean chargesEnabled;
    private Boolean payoutsEnabled;
    private Boolean detailsSubmitted;
    private String businessType; // individual, company
    private String businessProfileName;
    private Map<String, Object> capabilities;
    private Map<String, Object> requirements;
    
    public StripeAccountDetailsDto() {
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getDefaultCurrency() {
        return defaultCurrency;
    }
    
    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Boolean getChargesEnabled() {
        return chargesEnabled;
    }
    
    public void setChargesEnabled(Boolean chargesEnabled) {
        this.chargesEnabled = chargesEnabled;
    }
    
    public Boolean getPayoutsEnabled() {
        return payoutsEnabled;
    }
    
    public void setPayoutsEnabled(Boolean payoutsEnabled) {
        this.payoutsEnabled = payoutsEnabled;
    }
    
    public Boolean getDetailsSubmitted() {
        return detailsSubmitted;
    }
    
    public void setDetailsSubmitted(Boolean detailsSubmitted) {
        this.detailsSubmitted = detailsSubmitted;
    }
    
    public String getBusinessType() {
        return businessType;
    }
    
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
    
    public String getBusinessProfileName() {
        return businessProfileName;
    }
    
    public void setBusinessProfileName(String businessProfileName) {
        this.businessProfileName = businessProfileName;
    }
    
    public Map<String, Object> getCapabilities() {
        return capabilities;
    }
    
    public void setCapabilities(Map<String, Object> capabilities) {
        this.capabilities = capabilities;
    }
    
    public Map<String, Object> getRequirements() {
        return requirements;
    }
    
    public void setRequirements(Map<String, Object> requirements) {
        this.requirements = requirements;
    }
}

