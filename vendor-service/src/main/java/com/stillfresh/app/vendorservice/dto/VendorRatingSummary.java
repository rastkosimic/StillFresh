package com.stillfresh.app.vendorservice.dto;

public class VendorRatingSummary {
    
    private Long vendorId;
    private Double averageRating;
    private Integer totalRatings;
    private Double averageCollectionProcessRating;
    private Double averageQualityRating;
    private Double averageQuantityRating;
    private Double averageVarietyRating;

    // Getters and Setters

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Double getAverageCollectionProcessRating() {
        return averageCollectionProcessRating;
    }

    public void setAverageCollectionProcessRating(Double averageCollectionProcessRating) {
        this.averageCollectionProcessRating = averageCollectionProcessRating;
    }

    public Double getAverageQualityRating() {
        return averageQualityRating;
    }

    public void setAverageQualityRating(Double averageQualityRating) {
        this.averageQualityRating = averageQualityRating;
    }

    public Double getAverageQuantityRating() {
        return averageQuantityRating;
    }

    public void setAverageQuantityRating(Double averageQuantityRating) {
        this.averageQuantityRating = averageQuantityRating;
    }

    public Double getAverageVarietyRating() {
        return averageVarietyRating;
    }

    public void setAverageVarietyRating(Double averageVarietyRating) {
        this.averageVarietyRating = averageVarietyRating;
    }
}

