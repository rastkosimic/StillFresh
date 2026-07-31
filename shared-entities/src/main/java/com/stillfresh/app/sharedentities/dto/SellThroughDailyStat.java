package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;

/** Daily sell-through: listed vs sold units for the same date. */
public class SellThroughDailyStat implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;
    private long unitsListed;
    private long unitsSold;
    /** 0–1, or -1 if unitsListed is 0. */
    private double sellThroughRate = -1;

    public SellThroughDailyStat() {}

    public SellThroughDailyStat(String date, long unitsListed, long unitsSold, double sellThroughRate) {
        this.date = date;
        this.unitsListed = unitsListed;
        this.unitsSold = unitsSold;
        this.sellThroughRate = sellThroughRate;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getUnitsListed() { return unitsListed; }
    public void setUnitsListed(long unitsListed) { this.unitsListed = unitsListed; }

    public long getUnitsSold() { return unitsSold; }
    public void setUnitsSold(long unitsSold) { this.unitsSold = unitsSold; }

    public double getSellThroughRate() { return sellThroughRate; }
    public void setSellThroughRate(double sellThroughRate) { this.sellThroughRate = sellThroughRate; }
}
