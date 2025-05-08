package com.stillfresh.app.sharedentities.offer.events;

import java.time.OffsetDateTime;

import com.stillfresh.app.sharedentities.dto.OfferDto;

public class OfferUpdateEvent {
	
	private OfferDto offerDto;
	
	private Long offerId;
    private Long vendorId;
    private String name;
    private String description;
    private double price;
    private double originalPrice;
    private int quantityAvailable;
    private String address;
    private String zipCode; 
    private double latitude; 
    private double longitude;
    private String businessType;
    private String dietaryInfo;
    private String allergenInfo;
    private OffsetDateTime pickupStartTime;
    private OffsetDateTime pickupEndTime;
    private String imageUrl;
    private double rating;
    private int reviewsCount;
    private OffsetDateTime expirationDate;

    public OfferUpdateEvent() {
    }
    
    public OfferUpdateEvent(Long vendorId, OfferDto offerDto) {
    	this.vendorId = vendorId;
    	this.offerDto = offerDto;
    }

	
	public OfferDto getOfferDto() {
		return offerDto;
	}

	public void setOfferDto(OfferDto offerDto) {
		this.offerDto = offerDto;
	}

	public Long getVendorId() {
		return vendorId;
	}

	public void setVendorId(Long vendorId) {
		this.vendorId = vendorId;
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

	public Long getOfferId() {
		return offerId;
	}

	public void setOfferId(Long offerId) {
		this.offerId = offerId;
	}
}
