package com.stillfresh.app.vendorservice.model;

import java.time.OffsetDateTime;
import java.util.List;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.interfaces.Account;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Vendor implements Account{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String username;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Address cannot be blank")
    private String address; // Full address for display

    @NotBlank(message = "Phone number cannot be blank")
    private String phone;

    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToOne(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private VendorVerificationToken vendorVerificationToken;

    private String businessType; // e.g., restaurant, bakery, supermarket, hotel

    @ElementCollection
    private List<String> operatingHours; // e.g., ["Mon-Fri: 9 AM - 5 PM", "Sat: 10 AM - 4 PM"]

    @ElementCollection
    private List<String> surplusFoodDetails; // e.g., ["Baked Goods", "Prepared Meals"]

    private String pricingInfo; // Pricing details for surplus food packages

    private String environmentalCertifications; // Eco-friendly practices or certifications

    private double averageRating; // User rating for the vendor

    private int reviewsCount; // Number of user reviews
    
    private String imageUrl;
    
    private String zipCode; // For approximate searches
    
    private double latitude; // For precise geo-location
    
    private double longitude; // For precise geo-location

    // Getters and Setters

    public Long getId() {
        return id;
    }

	public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public VendorVerificationToken getVendorVerificationToken() {
        return vendorVerificationToken;
    }

    public void setVendorVerificationToken(VendorVerificationToken vendorVerificationToken) {
        this.vendorVerificationToken = vendorVerificationToken;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public List<String> getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(List<String> operatingHours) {
        this.operatingHours = operatingHours;
    }

    public List<String> getSurplusFoodDetails() {
        return surplusFoodDetails;
    }

    public void setSurplusFoodDetails(List<String> surplusFoodDetails) {
        this.surplusFoodDetails = surplusFoodDetails;
    }

    public String getPricingInfo() {
        return pricingInfo;
    }

    public void setPricingInfo(String pricingInfo) {
        this.pricingInfo = pricingInfo;
    }

    public String getEnvironmentalCertifications() {
        return environmentalCertifications;
    }

    public void setEnvironmentalCertifications(String environmentalCertifications) {
        this.environmentalCertifications = environmentalCertifications;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

	public boolean isActive() {
		return this.status == Status.ACTIVE;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
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
}

