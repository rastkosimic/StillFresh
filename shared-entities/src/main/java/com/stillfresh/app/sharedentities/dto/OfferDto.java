package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferDto implements Serializable{
    
	private static final long serialVersionUID = 1L;

	private Long id;  // Offer ID for reference
	
	private String vendorName;

    private String name;  // Offer name/title
    
    private String description;  // Brief description of the offer
    
    private double price;  // Discounted price of the offer
    
    private double originalPrice;  // Original price before discount
    
    private int quantityAvailable;  // Number of items available
    
    private String dietaryInfo;  // Optional dietary information (e.g., calorie count)
    
    private String allergenInfo;  // Optional allergen information (e.g., contains gluten)
    
    private String imageUrl;  // Optional URL of the offer image
    
    private double rating;  // Average user rating for the offer/vendor
    
    private int reviewsCount;  // Total number of user reviews
    
    private OffsetDateTime expirationDate;  // When the offer expires
    
    private boolean active;
    
    private OffsetDateTime createdAt;
    
	private String address;  // Address of the offer location
	
	private String zipCode;  // Zip code for approximate searches
	
	private double latitude;  // Precise latitude for geo-location
	
	private double longitude;  // Precise longitude for geo-location
	
	private String businessType;  // Type of business associated with the offer
	
	private OffsetDateTime pickupStartTime;  // Start time for offer pickup
	
	private OffsetDateTime pickupEndTime;  // End time for offer pickup
    
    public OfferDto() {}   
    

	public OfferDto(Long id, String vendorName, String name, String description, double price, double originalPrice, int quantityAvailable,
			String dietaryInfo, String allergenInfo, String imageUrl, double rating, int reviewsCount,
			OffsetDateTime expirationDate, boolean active, OffsetDateTime createdAt, String address, String zipCode,
			double latitude, double longitude, String businessType, OffsetDateTime pickupStartTime,
			OffsetDateTime pickupEndTime) {
		super();
		this.id = id;
		this.vendorName = vendorName;
		this.name = name;
		this.description = description;
		this.price = price;
		this.originalPrice = originalPrice;
		this.quantityAvailable = quantityAvailable;
		this.dietaryInfo = dietaryInfo;
		this.allergenInfo = allergenInfo;
		this.imageUrl = imageUrl;
		this.rating = rating;
		this.reviewsCount = reviewsCount;
		this.expirationDate = expirationDate;
		this.active = active;
		this.createdAt = createdAt;
		this.address = address;
		this.zipCode = zipCode;
		this.latitude = latitude;
		this.longitude = longitude;
		this.businessType = businessType;
		this.pickupStartTime = pickupStartTime;
		this.pickupEndTime = pickupEndTime;
	}

	// Getters and setters

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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
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

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
    
    
}
