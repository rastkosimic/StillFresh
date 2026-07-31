package com.stillfresh.app.paymentservice.service.executor;

/**
 * Strategy interface for executing outbound bank transfers to vendors.
 * <p>
 * Implementations must be idempotent: if a transfer with the same
 * {@link PayoutTransferRequest#getIdempotencyKey()} has already been submitted,
 * the executor must return a success result (with the original reference) rather
 * than issuing a duplicate transfer.
 * <p>
 * Active implementation is selected via {@code payout.rail}:
 * <ul>
 *   <li>{@code stub}     — StubBankTransferExecutor (default, dev/test)</li>
 *   <li>{@code sepa-xml} — SepaXmlExportExecutor (generates pain.001 file for manual upload)</li>
 * </ul>
 * These synchronous executors are exposed to the payout pipeline through
 * {@link com.stillfresh.app.paymentservice.service.rail.SyncExecutorPayoutRail};
 * asynchronous bank APIs (CMIplus) implement
 * {@link com.stillfresh.app.paymentservice.service.rail.PayoutRail} directly.
 */
public interface BankTransferExecutor {

    PayoutTransferResult execute(PayoutTransferRequest request);
}
