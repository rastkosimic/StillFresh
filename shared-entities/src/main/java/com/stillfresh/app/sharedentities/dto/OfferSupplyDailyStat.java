package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;

/** Daily units listed for sell-through trend charts. */
public class OfferSupplyDailyStat implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;
    private long unitsListed;

    public OfferSupplyDailyStat() {}

    public OfferSupplyDailyStat(String date, long unitsListed) {
        this.date = date;
        this.unitsListed = unitsListed;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getUnitsListed() { return unitsListed; }
    public void setUnitsListed(long unitsListed) { this.unitsListed = unitsListed; }
}
