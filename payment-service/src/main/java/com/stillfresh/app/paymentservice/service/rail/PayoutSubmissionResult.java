package com.stillfresh.app.paymentservice.service.rail;

/**
 * Result of submitting a vendor transfer to a {@link PayoutRail}.
 */
public class PayoutSubmissionResult {

    public enum Outcome {
        /** Transfer confirmed complete by a synchronous rail. */
        COMPLETED,
        /** Transfer accepted by the bank; awaiting asynchronous confirmation. */
        SUBMITTED,
        /** Submission rejected or errored. */
        FAILED
    }

    private final Outcome outcome;
    /** Bank message ID used to poll status later (async rails). */
    private final String bankMessageId;
    /** Bank-assigned transaction / file reference. */
    private final String externalReference;
    private final String errorMessage;

    private PayoutSubmissionResult(Outcome outcome, String bankMessageId,
                                   String externalReference, String errorMessage) {
        this.outcome = outcome;
        this.bankMessageId = bankMessageId;
        this.externalReference = externalReference;
        this.errorMessage = errorMessage;
    }

    public static PayoutSubmissionResult completed(String externalReference) {
        return new PayoutSubmissionResult(Outcome.COMPLETED, null, externalReference, null);
    }

    public static PayoutSubmissionResult submitted(String bankMessageId, String externalReference) {
        return new PayoutSubmissionResult(Outcome.SUBMITTED, bankMessageId, externalReference, null);
    }

    public static PayoutSubmissionResult failed(String errorMessage) {
        return new PayoutSubmissionResult(Outcome.FAILED, null, null, errorMessage);
    }

    public Outcome getOutcome() { return outcome; }
    public String getBankMessageId() { return bankMessageId; }
    public String getExternalReference() { return externalReference; }
    public String getErrorMessage() { return errorMessage; }
}
