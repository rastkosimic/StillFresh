package com.stillfresh.app.vendorservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stillfresh.app.sharedentities.enums.ManualPayoutMethod;

/**
 * Response DTO for MoR vendor banking data.
 * <p>
 * Security notes:
 * <ul>
 *   <li>Account number and IBAN are returned MASKED (last 4 characters only).</li>
 *   <li>Raw account number / raw IBAN are NEVER returned to the client.</li>
 *   <li>Full holder name, bank name and SWIFT code are returned as-is (non-secret identifiers).</li>
 *   <li>{@code hasBankDetails} lets the UI decide whether to render an "add" vs "edit" form.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MorBankDetailsResponse {

    private boolean hasBankDetails;
    private String holderName;
    private String bankName;
    private String swiftCode;
    private String accountNumberMasked;
    private String ibanMasked;
    private ManualPayoutMethod manualPayoutMethod;

    public MorBankDetailsResponse() {
    }

    public boolean isHasBankDetails() {
        return hasBankDetails;
    }

    public void setHasBankDetails(boolean hasBankDetails) {
        this.hasBankDetails = hasBankDetails;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public void setAccountNumberMasked(String accountNumberMasked) {
        this.accountNumberMasked = accountNumberMasked;
    }

    public String getIbanMasked() {
        return ibanMasked;
    }

    public void setIbanMasked(String ibanMasked) {
        this.ibanMasked = ibanMasked;
    }

    public ManualPayoutMethod getManualPayoutMethod() {
        return manualPayoutMethod;
    }

    public void setManualPayoutMethod(ManualPayoutMethod manualPayoutMethod) {
        this.manualPayoutMethod = manualPayoutMethod;
    }
}
