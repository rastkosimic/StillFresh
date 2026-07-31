package com.stillfresh.app.offerservice.service;

import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.iakovlev.timeshape.TimeZoneEngine;

/**
 * Offline service to map latitude/longitude to an IANA time zone (ZoneId).
 * Uses Timeshape time zone polygons, so it does not require external APIs.
 *
 * Important: For correctness, all pickup slot computations should use the vendor's ZoneId.
 */
@Service
public class TimeZoneDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(TimeZoneDetectionService.class);

    // Timeshape loads TZ polygons at startup
    private final TimeZoneEngine engine;

    // Cache by roughly-rounded coordinate buckets to avoid repeated polygon lookups
    private final ConcurrentHashMap<Long, ZoneId> cache = new ConcurrentHashMap<>();

    // 0.01 degree buckets (~1km lat); good tradeoff between accuracy and cache hit rate
    private static final int SCALE = 100;

    public TimeZoneDetectionService() {
        this.engine = TimeZoneEngine.initialize();
        logger.info("TimeZoneDetectionService initialized (Timeshape engine ready).");
    }

    public ZoneId getZoneId(double latitude, double longitude) {
        long key = cacheKey(latitude, longitude);
        ZoneId cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        Optional<ZoneId> zone = engine.query(latitude, longitude);
        ZoneId zoneId = zone.orElseGet(() -> {
            logger.warn("Could not determine timezone for lat={}, lon={}. Falling back to UTC.", latitude, longitude);
            return ZoneId.of("UTC");
        });

        cache.put(key, zoneId);
        return zoneId;
    }

    private long cacheKey(double latitude, double longitude) {
        int latKey = (int) Math.round(latitude * SCALE);
        int lonKey = (int) Math.round(longitude * SCALE);
        return (((long) latKey) << 32) ^ (lonKey & 0xffffffffL);
    }
}


