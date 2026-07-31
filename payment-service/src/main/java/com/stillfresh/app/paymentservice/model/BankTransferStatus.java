package com.stillfresh.app.paymentservice.model;

public enum BankTransferStatus {
    /** Awaiting customer to send the transfer. */
    PENDING_TRANSFER,
    /** Transfer received in the platform bank account; awaiting admin confirmation. */
    RECEIVED,
    /** Admin confirmed receipt; ledger entries written, vendor credited. */
    CONFIRMED,
    /** Order was cancelled before confirmation; no money movement. */
    CANCELLED
}
