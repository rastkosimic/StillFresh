package com.stillfresh.app.sharedentities.payment.events;

/**
 * Event sent from vendor-service to payment-service with vendor payment information
 */
public class VendorPaymentInfoResponseEvent {
    private String requestId;
    private Long vendorId;
    private boolean success;
    private String errorMessage;
    private String payoutModel; // "CONNECT" or "MOR"
    private String stripeAccountId; // Stripe Connect account ID (null for MoR vendors)
    private String ibanNumber;       // Bank IBAN for MoR vendors
    private String bankName;         // Bank name for MoR vendors
    private String accountHolderName; // Account holder name for MoR vendors
    private String accountNumber;    // Domestic bank account number (e.g. Serbian format) for MoR vendors
    private String bankCode;         // Bank code / BIC-SWIFT for MoR vendors

    public VendorPaymentInfoResponseEvent() {}

    public VendorPaymentInfoResponseEvent(String requestId, Long vendorId, boolean success) {
        this.requestId = requestId;
        this.vendorId = vendorId;
        this.success = success;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPayoutModel() {
        return payoutModel;
    }

    public void setPayoutModel(String payoutModel) {
        this.payoutModel = payoutModel;
    }

    public String getStripeAccountId() {
        return stripeAccountId;
    }

    public void setStripeAccountId(String stripeAccountId) {
        this.stripeAccountId = stripeAccountId;
    }

    public String getIbanNumber() {
        return ibanNumber;
    }

    public void setIbanNumber(String ibanNumber) {
        this.ibanNumber = ibanNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
}

