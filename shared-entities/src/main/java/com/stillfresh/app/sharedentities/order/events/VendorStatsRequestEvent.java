package com.stillfresh.app.sharedentities.order.events;

public class VendorStatsRequestEvent {
    private Long vendorId;
    private String from; // ISO-8601 string or null
    private String to;   // ISO-8601 string or null
    private String correlationId;

    public VendorStatsRequestEvent() {}

    public VendorStatsRequestEvent(Long vendorId, String from, String to, String correlationId) {
        this.vendorId = vendorId;
        this.from = from;
        this.to = to;
        this.correlationId = correlationId;
    }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}

