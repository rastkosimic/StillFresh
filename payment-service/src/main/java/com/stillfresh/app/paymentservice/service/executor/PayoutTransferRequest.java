package com.stillfresh.app.paymentservice.service.executor;

/**
 * Immutable request sent to the BankTransferExecutor for a single vendor payout.
 */
public class PayoutTransferRequest {

    private final Long vendorPayoutItemId;
    private final String idempotencyKey;
    private final String targetIban;
    private final String targetBankName;
    private final String targetAccountHolder;
    /** Domestic account number (e.g. Serbian format); may be null when an IBAN is present. */
    private final String targetAccountNumber;
    /** Bank code / BIC-SWIFT; may be null. */
    private final String targetBankCode;
    private final long amountCents;
    private final String currency;
    /** Human-readable payment description shown on the vendor's bank statement. */
    private final String description;

    public PayoutTransferRequest(Long vendorPayoutItemId, String idempotencyKey,
                                  String targetIban, String targetBankName,
                                  String targetAccountHolder,
                                  String targetAccountNumber, String targetBankCode,
                                  long amountCents,
                                  String currency, String description) {
        this.vendorPayoutItemId = vendorPayoutItemId;
        this.idempotencyKey = idempotencyKey;
        this.targetIban = targetIban;
        this.targetBankName = targetBankName;
        this.targetAccountHolder = targetAccountHolder;
        this.targetAccountNumber = targetAccountNumber;
        this.targetBankCode = targetBankCode;
        this.amountCents = amountCents;
        this.currency = currency;
        this.description = description;
    }

    public Long getVendorPayoutItemId() { return vendorPayoutItemId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getTargetIban() { return targetIban; }
    public String getTargetBankName() { return targetBankName; }
    public String getTargetAccountHolder() { return targetAccountHolder; }
    public String getTargetAccountNumber() { return targetAccountNumber; }
    public String getTargetBankCode() { return targetBankCode; }
    public long getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
}
