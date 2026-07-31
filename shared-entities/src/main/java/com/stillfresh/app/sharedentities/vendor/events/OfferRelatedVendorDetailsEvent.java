package com.stillfresh.app.sharedentities.vendor.events;

public class OfferRelatedVendorDetailsEvent {
    private Long id;
    // ===== Vendor display fields (snapshot) =====
    private String locationName;   // Per-location label
    private String chainName;      // Chain/brand label (null for unique vendors)
    private String website;        // Vendor website URL
    private String vendorImageUrl; // Vendor profile/logo image URL
    private String address;
    private String zipCode;
    private double latitude;
    private double longitude;
    private String businessType;
    private int reviewsCount;
    private double rating;
    private String country; // ISO 2-letter country code (e.g., "RS", "DE", "US")
    
    public OfferRelatedVendorDetailsEvent() {}

    public OfferRelatedVendorDetailsEvent(Long id, String locationName, String chainName, String website, String vendorImageUrl,
                                          String address, String zipCode, double latitude, double longitude,
                                          String businessType, int reviewsCount, double rating, String country) {
        this.id = id;
        this.locationName = locationName;
        this.chainName = chainName;
        this.website = website;
        this.vendorImageUrl = vendorImageUrl;
        this.address = address;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.businessType = businessType;
        this.reviewsCount = reviewsCount;
        this.rating = rating;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getVendorImageUrl() {
        return vendorImageUrl;
    }

    public void setVendorImageUrl(String vendorImageUrl) {
        this.vendorImageUrl = vendorImageUrl;
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

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
