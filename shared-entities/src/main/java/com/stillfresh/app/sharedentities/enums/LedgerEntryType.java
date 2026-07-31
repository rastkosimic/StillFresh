package com.stillfresh.app.sharedentities.enums;

public enum LedgerEntryType {
    /** Money owed to the vendor from a completed sale (net after platform fee). */
    VENDOR_CREDIT,
    /** Platform fee income earned on a completed sale. */
    PLATFORM_FEE_INCOME,
    /** Debit applied when a payout is executed to reduce the vendor's balance. */
    PAYOUT_DEBIT,
    /** Credit restoring the vendor's balance after a payout was rejected by the bank. */
    PAYOUT_REVERSAL,
    /** Manual correction entry created by an admin. */
    ADJUSTMENT
}
