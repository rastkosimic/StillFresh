package com.stillfresh.app.offerservice.model;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.stillfresh.app.sharedentities.enums.OfferCategory;

@Entity
@Table(name = "offers", indexes = {
    @Index(name = "idx_offer_geo", columnList = "latitude, longitude"),
    @Index(name = "idx_offer_active_geo", columnList = "active, latitude, longitude")
})
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long vendorId;

    @Column(nullable = false)
    private String name;

    // ===== Vendor display snapshot (copied from Vendor at create/update time) =====
    @Column(name = "location_name", nullable = true)
    private String locationName;

    @Column(name = "chain_name", nullable = true)
    private String chainName;

    @Column(nullable = true, length = 500)
    private String website;

    @Column(name = "vendor_image_url", nullable = true, length = 500)
    private String vendorImageUrl;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private double originalPrice;

    @Column(nullable = false)
    private int quantityAvailable;

    /** Original quantity when the offer was first published — never updated. Used to compute sell-through rate. */
    @Column(name = "original_quantity", nullable = false, updatable = false)
    private int originalQuantity;

    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false)
    private String zipCode; // For approximate searches
    
    @Column(nullable = false)
    private double latitude; // For precise geo-location
    
    @Column(nullable = false)
    private double longitude; // For precise geo-location

    @Column(nullable = true)  // Temporarily nullable for migration - will be set to NOT NULL after migration
    private String currency; // ISO currency code (e.g., "EUR", "RSD", "USD")

	@Column(nullable = false)
    private String businessType;

    @Column(nullable = true)  // Temporarily nullable for migration
    @Enumerated(EnumType.STRING)
    private OfferCategory category;

    @Column(nullable = true)
    private String dietaryInfo;

    @Column(nullable = true)
    private String allergenInfo;

    @Column(nullable = true) // Temporarily nullable for migration (Option B)
    private LocalDate pickupDate;

    @Column(nullable = false)
    private LocalTime pickupStartTime;

    @Column(nullable = false)
    private LocalTime pickupEndTime;

    @Column(nullable = true)
    private String imageUrl;

    @Column(nullable = true)
    private double rating;

    @Column(nullable = true)
    private int reviewsCount;

    @Column(nullable = true)
    private OffsetDateTime expirationDate;
    
    @Column(nullable = true)
    private LocalDate expiredAt;  // Date when offer expired (vendor's timezone)
    
    @Column(nullable = true)
    private LocalDate soldOutAt;   // Date when offer was sold out (vendor's timezone)
    
    @Column(nullable = false)
    private boolean active; // Indicates if the offer is active or not
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        // Snapshot the initial quantity so sell-through rate can be computed later
        if (this.originalQuantity == 0) {
            this.originalQuantity = this.quantityAvailable;
        }
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public int getOriginalQuantity() {
		return originalQuantity;
	}

	public void setOriginalQuantity(int originalQuantity) {
		this.originalQuantity = originalQuantity;
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

	public LocalDate getExpiredAt() {
		return expiredAt;
	}

	public void setExpiredAt(LocalDate expiredAt) {
		this.expiredAt = expiredAt;
	}

	public LocalDate getSoldOutAt() {
		return soldOutAt;
	}

	public void setSoldOutAt(LocalDate soldOutAt) {
		this.soldOutAt = soldOutAt;
	}

}
