package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayoutSubmissionServiceBankDetailsTest {

    private final PayoutSubmissionService service = new PayoutSubmissionService();

    @Test
    void acceptsIban() {
        VendorPayoutItem item = new VendorPayoutItem();
        item.setTargetIban("RS35160000000000000000000");
        assertTrue(service.hasValidBankDetails(item));
    }

    @Test
    void acceptsDomesticAccount() {
        VendorPayoutItem item = new VendorPayoutItem();
        item.setTargetAccountNumber("160-123456-78");
        item.setTargetBankCode("RZBSRSBG");
        assertTrue(service.hasValidBankDetails(item));
    }

    @Test
    void rejectsMissingDetails() {
        VendorPayoutItem item = new VendorPayoutItem();
        assertFalse(service.hasValidBankDetails(item));
    }
}
