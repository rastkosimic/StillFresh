package com.stillfresh.app.sharedentities.config;

/**
 * Packages Spring Kafka is allowed to deserialize from JSON type headers.
 *
 * <p>Spring Kafka 3.2 matches the class's <em>immediate</em> package only. Trusting
 * {@code com.stillfresh.app.sharedentities} therefore does not cover
 * {@code com.stillfresh.app.sharedentities.offer.events.OfferUpdateEvent}. {@code *} is
 * rejected because it would deserialize arbitrary types from the broker.
 */
public final class KafkaTrustedPackages {

    public static final String VALUE =
            "com.stillfresh.app.sharedentities,"
            + "com.stillfresh.app.sharedentities.dto,"
            + "com.stillfresh.app.sharedentities.enums,"
            + "com.stillfresh.app.sharedentities.offer.events,"
            + "com.stillfresh.app.sharedentities.order.events,"
            + "com.stillfresh.app.sharedentities.payment.events,"
            + "com.stillfresh.app.sharedentities.shared.events,"
            + "com.stillfresh.app.sharedentities.user.events,"
            + "com.stillfresh.app.sharedentities.vendor.events";

    private KafkaTrustedPackages() {
    }
}
