package com.stillfresh.app.offerservice.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "offer_supply_events", indexes = {
    @Index(name = "idx_ose_vendor_recorded", columnList = "vendor_id, recorded_at"),
    @Index(name = "idx_ose_offer_recorded", columnList = "offer_id, recorded_at")
})
public class OfferSupplyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "quantity_units", nullable = false)
    private int quantityUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private OfferSupplyEventType eventType;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt = OffsetDateTime.now();

    public OfferSupplyEvent() {}

    public Long getId() { return id; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public int getQuantityUnits() { return quantityUnits; }
    public void setQuantityUnits(int quantityUnits) { this.quantityUnits = quantityUnits; }

    public OfferSupplyEventType getEventType() { return eventType; }
    public void setEventType(OfferSupplyEventType eventType) { this.eventType = eventType; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}
