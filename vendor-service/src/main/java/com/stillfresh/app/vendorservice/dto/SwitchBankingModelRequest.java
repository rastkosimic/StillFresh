package com.stillfresh.app.vendorservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for switching banking model
 */
public class SwitchBankingModelRequest {
    
    @NotNull(message = "Banking model is required")
    private BankingModel bankingModel;
    
    public enum BankingModel {
        SHARED,      // All locations share the same bank account
        INDIVIDUAL   // Each location has its own bank account
    }
    
    // Getters and Setters
    
    public BankingModel getBankingModel() {
        return bankingModel;
    }
    
    public void setBankingModel(BankingModel bankingModel) {
        this.bankingModel = bankingModel;
    }
}

