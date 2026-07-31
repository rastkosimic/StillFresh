package com.stillfresh.app.orderservice.service;

import org.springframework.stereotype.Service;

/**
 * Geo-fence helper for the anti-bypass fraud engine. Computes great-circle distance between two
 * coordinates (Haversine) and decides whether a point is within a threshold of the pickup location.
 */
@Service
public class GeoFenceService {

    /** Default proximity threshold (metres) under which a cancellation is treated as suspicious. */
    public static final double BYPASS_THRESHOLD_METERS = 50.0;

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * Great-circle distance between two lat/long points, in metres.
     */
    public double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Returns true if the user is within {@link #BYPASS_THRESHOLD_METERS} of the pickup location.
     */
    public boolean isWithinBypassRange(double userLat, double userLon, double pickupLat, double pickupLon) {
        return distanceMeters(userLat, userLon, pickupLat, pickupLon) < BYPASS_THRESHOLD_METERS;
    }
}
