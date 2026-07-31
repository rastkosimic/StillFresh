package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;

public class OfferSupplyBreakdown implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long offerId;
    private long unitsListed;

    public OfferSupplyBreakdown() {}

    public OfferSupplyBreakdown(Long offerId, long unitsListed) {
        this.offerId = offerId;
        this.unitsListed = unitsListed;
    }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public long getUnitsListed() { return unitsListed; }
    public void setUnitsListed(long unitsListed) { this.unitsListed = unitsListed; }
}
