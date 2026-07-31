package com.stillfresh.app.paymentservice.service.rail.pain001;

import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;

/**
 * Strategy for building ISO 20022 pain.001.001.03 Credit Transfer Initiation
 * messages. Different rails / markets need different payment type information:
 * <ul>
 *   <li>{@link SepaPain001Builder} — SEPA service level (EUR zone, file upload)</li>
 *   <li>{@link DomesticLcyPain001Builder} — domestic local-currency transfers
 *       (Serbian RSD via CMIplus), no SEPA service level, supports plain
 *       account numbers in addition to IBANs</li>
 * </ul>
 * A future Serbia-specific rail can add its own builder without touching the
 * payout orchestration.
 */
public interface Pain001MessageBuilder {

    /**
     * Builds a single-transaction pain.001 document for the given transfer.
     * The request's idempotency key is used as PmtInfId / EndToEndId so the
     * bank can deduplicate resubmissions.
     */
    String build(PayoutTransferRequest request);
}
