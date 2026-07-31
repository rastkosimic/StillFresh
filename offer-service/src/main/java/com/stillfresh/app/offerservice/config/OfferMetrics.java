package com.stillfresh.app.offerservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Lightweight metrics for key offer endpoints to verify performance and detect regressions.
 * Records DB call duration and in-memory processing (filter + sort + DTO mapping) for
 * /offers/nearby and /offers.
 */
@Component
public class OfferMetrics {

    public static final String ENDPOINT_NEARBY = "nearby";
    public static final String ENDPOINT_ALL = "all";
    public static final String ENDPOINT_BY_CATEGORY = "by_category";

    private final MeterRegistry meterRegistry;

    @Autowired
    public OfferMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record duration of the repository (DB) call for an offer list endpoint.
     */
    public void recordOfferListDb(String endpoint, long timeMs) {
        Timer.builder("offer.list.db")
                .description("Time spent in DB query for offer list endpoints")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record duration of in-memory processing (filter + sort + DTO mapping) for an offer list endpoint.
     */
    public void recordOfferListInMemory(String endpoint, long timeMs) {
        Timer.builder("offer.list.in_memory")
                .description("Time spent in-memory (filter, sort, DTO mapping) for offer list endpoints")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS);
    }
}
