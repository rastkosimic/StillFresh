package com.stillfresh.app.paymentservice.model;

/** Tracks async AllSecure preauthorization state for client polling. */
public enum AuthorizationStatus {
    /** Offer validation / preauth in flight; no transaction row yet or awaiting gateway. */
    PROCESSING,
    /** 3DS step-up required — client must open {@code redirectUrl}. */
    AUTHENTICATION_REQUIRED,
    /** Preauth succeeded; {@code PaymentSuccessEvent} published (order should exist). */
    AUTHORIZED,
    /** Preauth failed or was declined. */
    FAILED
}
