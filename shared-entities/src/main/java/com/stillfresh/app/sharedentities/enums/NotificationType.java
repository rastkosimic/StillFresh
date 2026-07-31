package com.stillfresh.app.sharedentities.enums;

public enum NotificationType {
    // Order related
    ORDER_CONFIRMED,
    ORDER_RECEIVED,        // For vendors
    ORDER_READY,
    ORDER_CANCELLED,
    ORDER_PICKED_UP,
    ORDER_EXPIRED,           // Reservation expired (pickup window passed)
    ORDER_PICKUP_REMINDER,   // Reminder: pick up by stated time (e.g. 1 hour before)

    // Offer related
    OFFER_AVAILABLE,
    OFFER_EXPIRING,
    OFFER_EXPIRED,
    OFFER_CREATED,
    OFFER_UPDATED,
    
    // Payment related
    PAYMENT_SUCCESSFUL,
    PAYMENT_FAILED,
    PAYMENT_PENDING,
    BANK_TRANSFER_INITIATED,  // Payment instructions for bank transfer orders
    BANK_TRANSFER_CONFIRMED,  // Bank transfer confirmed, vendor will be paid out
    
    // Account related
    ACCOUNT_VERIFIED,
    PASSWORD_RESET,
    PROFILE_UPDATED,
    
    // Banking/Payment related
    BANKING_MODEL_CHANGED,  // Notifies chain locations when banking model is switched
    
    // System
    SYSTEM_ALERT,
    MAINTENANCE_NOTICE
}


