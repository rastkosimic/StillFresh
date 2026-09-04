package com.stillfresh.app.vendorservice.dto;

import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Aggregated sales statistics for all selling locations in a chain.
 * HQ VENDOR_ADMIN and SUPER_ADMIN only.
 */
public class ChainLocationStatsResponse {

    private String chainId;
    private String chainName;
    private OffsetDateTime from;
    private OffsetDateTime to;
    /** Rolled-up totals across all locations (no merged offer breakdown). */
    private VendorStatsResponse chainTotals;
    private List<LocationStatsEntry> locations;

    public static class LocationStatsEntry {
        private Long vendorId;
        private String locationName;
        private Boolean isHeadquarters;
        private VendorStatsResponse stats;
        /** Set when order-service stats could not be loaded for this location. */
        private String error;

        public Long getVendorId() {
            return vendorId;
        }

        public void setVendorId(Long vendorId) {
            this.vendorId = vendorId;
        }

        public String getLocationName() {
            return locationName;
        }

        public void setLocationName(String locationName) {
            this.locationName = locationName;
        }

        public Boolean getIsHeadquarters() {
            return isHeadquarters;
        }

        public void setIsHeadquarters(Boolean isHeadquarters) {
            this.isHeadquarters = isHeadquarters;
        }

        public VendorStatsResponse getStats() {
            return stats;
        }

        public void setStats(VendorStatsResponse stats) {
            this.stats = stats;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public void setFrom(OffsetDateTime from) {
        this.from = from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public void setTo(OffsetDateTime to) {
        this.to = to;
    }

    public VendorStatsResponse getChainTotals() {
        return chainTotals;
    }

    public void setChainTotals(VendorStatsResponse chainTotals) {
        this.chainTotals = chainTotals;
    }

    public List<LocationStatsEntry> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationStatsEntry> locations) {
        this.locations = locations;
    }
}
