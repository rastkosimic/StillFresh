package com.stillfresh.app.vendorservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for setting banking model (SHARED or INDIVIDUAL)
 */
public class BankingModelRequest {
    
    @NotNull(message = "Banking model is required")
    private BankingModel bankingModel;
    
    private String country;  // Required for UNIQUE vendors, optional for CHAIN vendors (should be set during headquarters step)
    
    public enum BankingModel {
        SHARED,      // All locations share the same bank account
        INDIVIDUAL   // Each location has its own bank account (franchise model)
    }
    
    // Getters and Setters
    
    public BankingModel getBankingModel() {
        return bankingModel;
    }
    
    public void setBankingModel(BankingModel bankingModel) {
        this.bankingModel = bankingModel;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
}

