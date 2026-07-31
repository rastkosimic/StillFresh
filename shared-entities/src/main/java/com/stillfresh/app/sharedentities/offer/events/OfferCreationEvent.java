package com.stillfresh.app.sharedentities.offer.events;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;

import com.stillfresh.app.sharedentities.enums.OfferCategory;

public class OfferCreationEvent {

    private Long vendorId;
    private String name;
    // ===== Vendor display fields (snapshot from Vendor at offer creation) =====
    private String locationName;   // Per-location label
    private String chainName;      // Chain/brand label (null for unique vendors)
    private String website;        // Vendor website URL
    private String vendorImageUrl; // Vendor profile/logo image URL
    private String description;
    private double price;
    private double originalPrice;
    private int quantityAvailable;
    private String address;
    private String zipCode; 
    private double latitude; 
    private double longitude;
    private String businessType;
    private OfferCategory category;
    private String dietaryInfo;
    private String allergenInfo;
    private LocalDate pickupDate;
    private LocalTime pickupStartTime;
    private LocalTime pickupEndTime;
    private String imageUrl; // Offer's own image URL (distinct from vendorImageUrl)
    private double rating;
    private int reviewsCount;
    private OffsetDateTime expirationDate;
    private String country; // ISO 2-letter country code (e.g., "RS", "DE", "US")

    public OfferCreationEvent() {
    }

	public OfferCreationEvent(Long vendorId, String locationName, String chainName, String website, String vendorImageUrl,
			String name, String description, double price, double originalPrice,
			int quantityAvailable, String address, String zipCode, double latitude, double longitude, String businessType,
			OfferCategory category, String dietaryInfo, String allergenInfo, LocalDate pickupDate, LocalTime pickupStartTime, LocalTime pickupEndTime,
			String imageUrl, double rating, int reviewsCount, OffsetDateTime expirationDate, String country) {
		super();
		this.vendorId = vendorId;
		this.locationName = locationName;
		this.chainName = chainName;
		this.website = website;
		this.vendorImageUrl = vendorImageUrl;
		this.name = name;
		this.description = description;
		this.price = price;
		this.originalPrice = originalPrice;
		this.quantityAvailable = quantityAvailable;
		this.address = address;
		this.zipCode = zipCode;
		this.latitude = latitude;
		this.longitude = longitude;
		this.businessType = businessType;
		this.category = category;
		this.dietaryInfo = dietaryInfo;
		this.allergenInfo = allergenInfo;
		this.pickupDate = pickupDate;
		this.pickupStartTime = pickupStartTime;
		this.pickupEndTime = pickupEndTime;
		this.imageUrl = imageUrl;
		this.rating = rating;
		this.reviewsCount = reviewsCount;
		this.expirationDate = expirationDate;
		this.country = country;
	}

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public double getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(double originalPrice) {
		this.originalPrice = originalPrice;
	}

	public int getQuantityAvailable() {
		return quantityAvailable;
	}

	public void setQuantityAvailable(int quantityAvailable) {
		this.quantityAvailable = quantityAvailable;
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

	public OfferCategory getCategory() {
		return category;
	}

	public void setCategory(OfferCategory category) {
		this.category = category;
	}

	public String getDietaryInfo() {
		return dietaryInfo;
	}

	public void setDietaryInfo(String dietaryInfo) {
		this.dietaryInfo = dietaryInfo;
	}

	public String getAllergenInfo() {
		return allergenInfo;
	}

	public void setAllergenInfo(String allergenInfo) {
		this.allergenInfo = allergenInfo;
	}
	
	public LocalDate getPickupDate() {
		return pickupDate;
	}
	
	public void setPickupDate(LocalDate pickupDate) {
		this.pickupDate = pickupDate;
	}

	public LocalTime getPickupStartTime() {
		return pickupStartTime;
	}

	public void setPickupStartTime(LocalTime pickupStartTime) {
		this.pickupStartTime = pickupStartTime;
	}

	public LocalTime getPickupEndTime() {
		return pickupEndTime;
	}

	public void setPickupEndTime(LocalTime pickupEndTime) {
		this.pickupEndTime = pickupEndTime;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public int getReviewsCount() {
		return reviewsCount;
	}

	public void setReviewsCount(int reviewsCount) {
		this.reviewsCount = reviewsCount;
	}

	public OffsetDateTime getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(OffsetDateTime expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

}
