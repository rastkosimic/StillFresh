package com.stillfresh.app.paymentservice.allsecure;

/**
 * Parsed asynchronous postback (&lt;callback&gt;) sent by AllSecure once a transaction reaches a final state.
 */
public class AllSecureCallback {

    public static final String RESULT_OK = "OK";
    public static final String RESULT_ERROR = "ERROR";

    private String result;
    private String referenceId;
    private String transactionId;
    private String purchaseId;
    private String transactionType;
    private String merchantMetaData;
    private String amount;
    private String currency;
    private String errorMessage;
    private String errorCode;

    /** customerData/identification echoed back (we set this to the username at request time). */
    private String customerIdentification;

    // Credit card data (returnData/creditcardData), when present
    private String cardType;
    private String cardHolder;
    private String cardFirstSixDigits;
    private String cardLastFourDigits;
    private String cardExpiryMonth;
    private String cardExpiryYear;

    public boolean isOk() { return RESULT_OK.equalsIgnoreCase(result); }
    public boolean isError() { return RESULT_ERROR.equalsIgnoreCase(result); }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPurchaseId() { return purchaseId; }
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getMerchantMetaData() { return merchantMetaData; }
    public void setMerchantMetaData(String merchantMetaData) { this.merchantMetaData = merchantMetaData; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getCustomerIdentification() { return customerIdentification; }
    public void setCustomerIdentification(String customerIdentification) { this.customerIdentification = customerIdentification; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }

    public String getCardFirstSixDigits() { return cardFirstSixDigits; }
    public void setCardFirstSixDigits(String cardFirstSixDigits) { this.cardFirstSixDigits = cardFirstSixDigits; }

    public String getCardLastFourDigits() { return cardLastFourDigits; }
    public void setCardLastFourDigits(String cardLastFourDigits) { this.cardLastFourDigits = cardLastFourDigits; }

    public String getCardExpiryMonth() { return cardExpiryMonth; }
    public void setCardExpiryMonth(String cardExpiryMonth) { this.cardExpiryMonth = cardExpiryMonth; }

    public String getCardExpiryYear() { return cardExpiryYear; }
    public void setCardExpiryYear(String cardExpiryYear) { this.cardExpiryYear = cardExpiryYear; }

    @Override
    public String toString() {
        return "AllSecureCallback{result='" + result + '\'' + ", transactionType='" + transactionType + '\''
                + ", referenceId='" + referenceId + '\'' + ", transactionId='" + transactionId + '\''
                + ", errorCode='" + errorCode + '\'' + ", errorMessage='" + errorMessage + '\'' + '}';
    }
}
