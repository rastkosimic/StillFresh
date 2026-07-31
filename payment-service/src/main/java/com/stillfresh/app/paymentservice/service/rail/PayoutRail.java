package com.stillfresh.app.paymentservice.service.rail;

import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;

/**
 * Strategy interface for outbound vendor payout rails (bank integrations).
 * <p>
 * A rail may be synchronous (transfer confirmed within {@link #submit}, e.g. the
 * dev stub or a manually uploaded SEPA file) or asynchronous (transfer accepted
 * by the bank API and confirmed later via {@link #pollStatus}, e.g. CMIplus
 * pain.001 submission confirmed by pain.002).
 * <p>
 * Implementations must be idempotent on
 * {@link PayoutTransferRequest#getIdempotencyKey()}: re-submitting the same key
 * must never move money twice.
 * <p>
 * The active rail is selected via {@code payout.rail}:
 * <ul>
 *   <li>{@code stub}     — dev/test, no real transfers (default)</li>
 *   <li>{@code sepa-xml} — generates pain.001 files for manual bank upload</li>
 *   <li>{@code cmiplus}  — Raiffeisen CMIplus Open APIs (pain.001 / pain.002)</li>
 * </ul>
 * A future Serbia-specific domestic rail plugs in by adding a new implementation;
 * no orchestration code changes are required.
 */
public interface PayoutRail {

    /**
     * Submits a single vendor transfer to the bank rail.
     * Returns a result whose status is either terminal (COMPLETED / FAILED for
     * synchronous rails) or SUBMITTED (asynchronous rails, confirmed later by
     * {@link #pollStatus}).
     */
    PayoutSubmissionResult submit(PayoutTransferRequest request);

    /**
     * Polls the bank for the current status of a previously SUBMITTED item.
     * Synchronous rails never leave items in SUBMITTED, so they may throw
     * {@link UnsupportedOperationException}.
     */
    PayoutStatusUpdate pollStatus(VendorPayoutItem item);

    /** Short identifier stored on each item (e.g. "STUB", "SEPA-XML", "CMIPLUS"). */
    String railType();
}
