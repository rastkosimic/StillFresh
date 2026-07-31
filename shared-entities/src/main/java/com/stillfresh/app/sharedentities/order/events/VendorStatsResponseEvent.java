package com.stillfresh.app.sharedentities.order.events;

import com.stillfresh.app.sharedentities.dto.VendorStatsResponse;

public class VendorStatsResponseEvent {
    private String correlationId;
    private boolean success;
    private String errorMessage;
    private VendorStatsResponse stats;

    public VendorStatsResponseEvent() {}

    public VendorStatsResponseEvent(String correlationId, boolean success, String errorMessage, VendorStatsResponse stats) {
        this.correlationId = correlationId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.stats = stats;
    }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public VendorStatsResponse getStats() { return stats; }
    public void setStats(VendorStatsResponse stats) { this.stats = stats; }
}

