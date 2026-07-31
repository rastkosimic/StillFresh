package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.PaymentTransaction;

/**
 * Issues a Serbian e-fiskalni fiscal receipt for a settled (captured) marketplace payment.
 * As StillFresh acts as an intermediary, receipts are issued under the "Prodaja preko posrednika"
 * (sale via intermediary) classification.
 *
 * <p>Implementations should be best-effort and must never throw into the settlement flow.</p>
 */
public interface FiscalReceiptService {

    /** Classification label required by the Serbian fiscalization rules for marketplace sales. */
    String SALE_VIA_INTERMEDIARY = "Prodaja preko posrednika";

    /**
     * Issue (or attempt to issue) a fiscal receipt for the given captured payment transaction.
     * No-op when fiscalization is disabled.
     */
    void issueReceipt(PaymentTransaction tx);
}
