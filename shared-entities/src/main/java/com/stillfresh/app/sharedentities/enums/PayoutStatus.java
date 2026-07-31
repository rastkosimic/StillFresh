package com.stillfresh.app.sharedentities.enums;

public enum PayoutStatus {
    /** Created but not yet scheduled for execution. */
    PENDING,
    /** Included in a payout batch, waiting for execution. */
    SCHEDULED,
    /** Admin has reviewed and approved the batch for execution. */
    APPROVED,
    /** Execution is in progress (transfer initiated). */
    IN_PROGRESS,
    /** Item accepted by the bank rail (e.g. pain.001 submitted); awaiting confirmation (pain.002). */
    SUBMITTED,
    /** Admin placed the batch/item on hold; the automatic pipeline skips it. */
    ON_HOLD,
    /** Admin cancelled the batch/item before submission to the bank. */
    CANCELLED,
    /** Successfully transferred to the vendor. */
    COMPLETED,
    /** Some items completed, some failed. */
    PARTIALLY_COMPLETED,
    /** Transfer failed; may be retried. */
    FAILED
}
