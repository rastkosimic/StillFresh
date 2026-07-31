package com.stillfresh.app.paymentservice.service.rail;

/**
 * Bank-reported status of a previously SUBMITTED payout item
 * (e.g. mapped from an ISO 20022 pain.002 status report).
 */
public class PayoutStatusUpdate {

    public enum Outcome {
        /** Still processing at the bank; keep the item in SUBMITTED. */
        PENDING,
        /** Transfer settled/confirmed (pain.002 ACSC/ACCC or equivalent). */
        COMPLETED,
        /** Transfer rejected by the bank (pain.002 RJCT); ledger must be reversed. */
        FAILED
    }

    private final Outcome outcome;
    private final String externalReference;
    private final String errorMessage;

    private PayoutStatusUpdate(Outcome outcome, String externalReference, String errorMessage) {
        this.outcome = outcome;
        this.externalReference = externalReference;
        this.errorMessage = errorMessage;
    }

    public static PayoutStatusUpdate pending() {
        return new PayoutStatusUpdate(Outcome.PENDING, null, null);
    }

    public static PayoutStatusUpdate completed(String externalReference) {
        return new PayoutStatusUpdate(Outcome.COMPLETED, externalReference, null);
    }

    public static PayoutStatusUpdate failed(String errorMessage) {
        return new PayoutStatusUpdate(Outcome.FAILED, null, errorMessage);
    }

    public Outcome getOutcome() { return outcome; }
    public String getExternalReference() { return externalReference; }
    public String getErrorMessage() { return errorMessage; }
}
