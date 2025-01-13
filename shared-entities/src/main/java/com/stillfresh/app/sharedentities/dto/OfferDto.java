package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferDto implements Serializable{
    
	private static final long serialVersionUID = 1L;

	private int id;  // Offer ID for reference

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
    
    public OfferDto() {}

    public OfferDto(int id, String name, String description, double price, double originalPrice, int quantityAvailable,
			String dietaryInfo, String allergenInfo, String imageUrl, double rating, int reviewsCount,
			OffsetDateTime expirationDate, boolean active, OffsetDateTime createdAt) {
		this.id = id;
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
	}

	// Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
    
    
}
