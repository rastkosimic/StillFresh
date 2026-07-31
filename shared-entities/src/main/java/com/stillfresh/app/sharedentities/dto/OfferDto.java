package com.stillfresh.app.sharedentities.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.stillfresh.app.sharedentities.enums.OfferCategory;
import com.stillfresh.app.sharedentities.enums.PickupDaySlot;
import com.stillfresh.app.sharedentities.enums.PickupMealSlot;
import com.stillfresh.app.sharedentities.jackson.MultiFormatLocalDateDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferDto implements Serializable{
    
	private static final long serialVersionUID = 1L;

	private Long id;  // Offer ID for reference

	private Long vendorId;

	// ===== Vendor display fields (snapshot) =====
	private String locationName;   // Per-location label (e.g. "Downtown branch")
	private String chainName;      // Brand/chain label (e.g. "Starbucks"); null for unique vendors
	private String website;        // Vendor website URL
	private String vendorImageUrl; // Vendor profile/logo image URL

    private String name;  // Offer name/title
    
    private String description;  // Brief description of the offer
    
    private double price;  // Discounted price of the offer
    
    private double originalPrice;  // Original price before discount
    
    private int quantityAvailable;  // Number of items available (current remaining)
    private int originalQuantity;   // Quantity when first published — for sell-through rate
    
    private String dietaryInfo;  // Optional dietary information (e.g., calorie count)
    
    private String allergenInfo;  // Optional allergen information (e.g., contains gluten)
    
    private String imageUrl;  // Optional URL of the offer's own image (distinct from vendorImageUrl)
    
    private double rating;  // Average user rating for the offer/vendor
    
    private int reviewsCount;  // Total number of user reviews
    
    private OffsetDateTime expirationDate;  // When the offer expires
    
    private boolean active;
    
    private OffsetDateTime createdAt;
    
	private String address;  // Address of the offer location
	
	private String zipCode;  // Zip code for approximate searches
	
	private double latitude;  // Precise latitude for geo-location
	
	private double longitude;  // Precise longitude for geo-location
	
	private String currency;  // ISO currency code (e.g., "EUR", "RSD", "USD")
	
	private String businessType;  // Type of business associated with the offer
	
	private OfferCategory category;  // Category of the offer (e.g., MEALS, GROCERIES)
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@JsonDeserialize(using = MultiFormatLocalDateDeserializer.class)
	private LocalDate pickupDate; // Pickup date selected by vendor (Option B). Accepts multiple input formats; outputs ISO yyyy-MM-dd.
	
	private LocalTime pickupStartTime;  // Start time for offer pickup
	
	private LocalTime pickupEndTime;  // End time for offer pickup
	
	// ===== Derived (non-persisted) fields for UI grouping =====
	private PickupDaySlot pickupDaySlot;   // TODAY / TOMORROW / FUTURE / PAST
	private PickupMealSlot pickupMealSlot; // BREAKFAST / LUNCH / DINNER / OTHER
	private boolean collectNow;            // true if now is within pickup window (today)
	
	// ===== Status flags for UI =====
	private boolean isExpired;      // true if offer has expired (expiredAt set)
	private boolean isSoldOut;      // true if offer has sold out (soldOutAt set)
	private boolean isGreyedOut;    // true if isExpired || isSoldOut
    
    public OfferDto() {}   
    

	public OfferDto(Long id, String locationName, String chainName, String website, String vendorImageUrl,
			String name, String description, double price, double originalPrice, int quantityAvailable,
			String dietaryInfo, String allergenInfo, String imageUrl, double rating, int reviewsCount,
			OffsetDateTime expirationDate, boolean active, OffsetDateTime createdAt, String address, String zipCode,
			double latitude, double longitude, String currency, String businessType, OfferCategory category, LocalTime pickupStartTime,
			LocalTime pickupEndTime) {
		super();
		this.id = id;
		this.locationName = locationName;
		this.chainName = chainName;
		this.website = website;
		this.vendorImageUrl = vendorImageUrl;
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
		this.currency = currency;
		this.businessType = businessType;
		this.category = category;
		this.pickupDate = null;
		this.pickupStartTime = pickupStartTime;
		this.pickupEndTime = pickupEndTime;
	}
	
	public OfferDto(Long id, String locationName, String chainName, String website, String vendorImageUrl,
			String name, String description, double price, double originalPrice, int quantityAvailable,
			String dietaryInfo, String allergenInfo, String imageUrl, double rating, int reviewsCount,
			OffsetDateTime expirationDate, boolean active, OffsetDateTime createdAt, String address, String zipCode,
			double latitude, double longitude, String currency, String businessType, OfferCategory category, LocalDate pickupDate,
			LocalTime pickupStartTime, LocalTime pickupEndTime) {
		this(id, locationName, chainName, website, vendorImageUrl, name, description, price, originalPrice,
				quantityAvailable, dietaryInfo, allergenInfo, imageUrl, rating, reviewsCount, expirationDate, active,
				createdAt, address, zipCode, latitude, longitude, currency, businessType, category,
				pickupStartTime, pickupEndTime);
		this.pickupDate = pickupDate;
	}

	// Getters and setters

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

    public int getOriginalQuantity() {
        return originalQuantity;
    }

    public void setOriginalQuantity(int originalQuantity) {
        this.originalQuantity = originalQuantity;
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

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
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
	
	public PickupDaySlot getPickupDaySlot() {
		return pickupDaySlot;
	}
	
	public void setPickupDaySlot(PickupDaySlot pickupDaySlot) {
		this.pickupDaySlot = pickupDaySlot;
	}
	
	public PickupMealSlot getPickupMealSlot() {
		return pickupMealSlot;
	}
	
	public void setPickupMealSlot(PickupMealSlot pickupMealSlot) {
		this.pickupMealSlot = pickupMealSlot;
	}
	
	public boolean isCollectNow() {
		return collectNow;
	}
	
	public void setCollectNow(boolean collectNow) {
		this.collectNow = collectNow;
	}

	public boolean isExpired() {
		return isExpired;
	}

	public void setExpired(boolean isExpired) {
		this.isExpired = isExpired;
	}

	public boolean isSoldOut() {
		return isSoldOut;
	}

	public void setSoldOut(boolean isSoldOut) {
		this.isSoldOut = isSoldOut;
	}

	public boolean isGreyedOut() {
		return isGreyedOut;
	}

	public void setGreyedOut(boolean isGreyedOut) {
		this.isGreyedOut = isGreyedOut;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
    
    
}
