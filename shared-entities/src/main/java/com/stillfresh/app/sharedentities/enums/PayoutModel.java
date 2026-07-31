package com.stillfresh.app.sharedentities.enums;

public enum PayoutModel {
    CONNECT,  // Stripe Connect - funds go directly to vendor via Stripe
    MOR       // Merchant of Record - platform holds funds, pays vendors manually
}

