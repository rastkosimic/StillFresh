package com.stillfresh.app.sharedentities.vendor.events;

import java.time.OffsetDateTime;

public class OfferRelatedVendorDetailsEvent {
    private Long id;
    private String vendorName;
    private String address;
    private String zipCode;
    private double latitude;
    private double longitude;
    private String businessType;
    private OffsetDateTime pickupStartTime;
    private OffsetDateTime pickupEndTime;
    private int reviewsCount;
    
    public OfferRelatedVendorDetailsEvent() {}

    public OfferRelatedVendorDetailsEvent(Long id, String vendorName, String address, String zipCode, double latitude, double longitude,
                                          String businessType, OffsetDateTime pickupStartTime, OffsetDateTime pickupEndTime,
                                          int reviewsCount) {
        this.id = id;
        this.vendorName = vendorName;
        this.address = address;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.businessType = businessType;
        this.pickupStartTime = pickupStartTime;
        this.pickupEndTime = pickupEndTime;
        this.reviewsCount = reviewsCount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public OffsetDateTime getPickupStartTime() {
        return pickupStartTime;
    }

    public void setPickupStartTime(OffsetDateTime pickupStartTime) {
        this.pickupStartTime = pickupStartTime;
    }

    public OffsetDateTime getPickupEndTime() {
        return pickupEndTime;
    }

    public void setPickupEndTime(OffsetDateTime pickupEndTime) {
        this.pickupEndTime = pickupEndTime;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }
}
