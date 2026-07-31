package com.stillfresh.app.paymentservice.dto;

import java.io.Serializable;

/**
 * DTO for customer payment methods (cards and bank accounts)
 */
public class CustomerPaymentMethodDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String paymentMethodId;
    private String type; // "card" or "us_bank_account"
    private Boolean isDefault;
    
    // Card-specific fields
    private String cardBrand; // visa, mastercard, amex, etc.
    private String cardLast4;
    private Long cardExpMonth;
    private Long cardExpYear;
    private String cardFunding; // credit, debit, prepaid, unknown
    
    // Bank account-specific fields
    private String bankAccountType; // checking, savings
    private String bankAccountLast4;
    private String bankName;
    private String bankAccountHolderType; // individual, company
    private String bankAccountStatus; // new, validated, verified, verification_failed, errored
    
    // Common fields
    private String country;
    private String currency;
    
    public CustomerPaymentMethodDto() {
    }
    
    // Getters and Setters
    public String getPaymentMethodId() {
        return paymentMethodId;
    }
    
    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public String getCardBrand() {
        return cardBrand;
    }
    
    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }
    
    public String getCardLast4() {
        return cardLast4;
    }
    
    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }
    
    public Long getCardExpMonth() {
        return cardExpMonth;
    }
    
    public void setCardExpMonth(Long cardExpMonth) {
        this.cardExpMonth = cardExpMonth;
    }
    
    public Long getCardExpYear() {
        return cardExpYear;
    }
    
    public void setCardExpYear(Long cardExpYear) {
        this.cardExpYear = cardExpYear;
    }
    
    public String getCardFunding() {
        return cardFunding;
    }
    
    public void setCardFunding(String cardFunding) {
        this.cardFunding = cardFunding;
    }
    
    public String getBankAccountType() {
        return bankAccountType;
    }
    
    public void setBankAccountType(String bankAccountType) {
        this.bankAccountType = bankAccountType;
    }
    
    public String getBankAccountLast4() {
        return bankAccountLast4;
    }
    
    public void setBankAccountLast4(String bankAccountLast4) {
        this.bankAccountLast4 = bankAccountLast4;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public String getBankAccountHolderType() {
        return bankAccountHolderType;
    }
    
    public void setBankAccountHolderType(String bankAccountHolderType) {
        this.bankAccountHolderType = bankAccountHolderType;
    }
    
    public String getBankAccountStatus() {
        return bankAccountStatus;
    }
    
    public void setBankAccountStatus(String bankAccountStatus) {
        this.bankAccountStatus = bankAccountStatus;
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
}

