package com.stillfresh.app.vendorservice.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.stillfresh.app.sharedentities.enums.ManualPayoutMethod;
import com.stillfresh.app.sharedentities.enums.OnboardingStatus;
import com.stillfresh.app.sharedentities.enums.PaymentProvider;
import com.stillfresh.app.sharedentities.enums.PayoutModel;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.interfaces.Account;

import java.math.BigDecimal;

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
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

@Entity
public class Vendor implements Account{

    @Id
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

    @JsonProperty(access = Access.WRITE_ONLY)
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToOne(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
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

    @Column(name = "bypass_strike_count", nullable = false)
    private int bypassStrikeCount = 0; // Anti-abuse: cancellations flagged as potential user-vendor bypass

    // Legal acceptance audit. Version is supplied by the client (document version displayed);
    // the timestamp is stamped server-side at acceptance.
    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "terms_version")
    private String termsVersion;

    @Column(name = "privacy_accepted_at")
    private LocalDateTime privacyAcceptedAt;

    @Column(name = "privacy_version")
    private String privacyVersion;

    private String imageUrl;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String aboutBusiness;

    private String contactPerson;
    
    private String zipCode; // For approximate searches (optional; front-end provides coordinates)
    
    @NotNull(message = "Latitude is required")
    private Double latitude; // For precise geo-location
    
    @NotNull(message = "Longitude is required")
    private Double longitude; // For precise geo-location
    
    @JsonIgnore
    private String stripeAccountId; // Stripe Connect account ID (e.g., "acct_xxxxx")
    
    @Enumerated(EnumType.STRING)
    private PayoutModel payoutModel; // CONNECT (Stripe Connect) or MOR (Merchant of Record)
    
    @Enumerated(EnumType.STRING)
    private PaymentProvider paymentProvider; // STRIPE or MOR
    
    private String country; // ISO 2-letter country code (e.g., "US", "RS", "DE")
    
    @JsonIgnore
    private Boolean stripeSupported; // Cached flag indicating if country supports Stripe
    
    // MoR (Merchant of Record) specific fields
    @JsonIgnore
    private BigDecimal balance; // Internal balance for MoR vendors (in cents, stored as decimal)
    
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private ManualPayoutMethod manualPayoutMethod; // BANK, WISE, etc. (for MOR vendors)
    
    // Bank account details for MoR vendors (stored securely)
    @JsonIgnore
    private String bankAccountHolderName;
    @JsonIgnore
    private String bankAccountNumber;
    @JsonIgnore
    private String bankName;
    @JsonIgnore
    private String bankSwiftCode; // BIC/SWIFT code
    @JsonIgnore
    private String bankIban; // IBAN if applicable
    
    // ========== Chain and Onboarding Fields ==========
    
    /**
     * Unique identifier for the chain (UUID format)
     * All locations in the same chain share the same chainId
     */
    private String chainId;
    
    /**
     * Brand/chain name (e.g., "McDonald's", "Starbucks")
     * Null for unique vendors
     */
    private String chainName;
    
    /**
     * Location identifier within the chain (e.g., "Downtown", "Airport", "123 Main St")
     * For unique vendors, this can be null or same as username
     */
    private String locationName;
    
    /**
     * True if this vendor is part of a chain (has multiple locations)
     */
    private Boolean isChainLocation;
    
    /**
     * True if this is the headquarters location for a chain
     * Headquarters is a selling location, not a corporate office
     */
    private Boolean isHeadquarters;
    
    /**
     * True if this is a unique/standalone vendor (can upgrade to chain later)
     */
    private Boolean isUniqueVendor;
    
    /**
     * Business registration ID or tax ID number
     * Used for business verification
     */
    private String businessRegistrationId;
    
    /**
     * Current onboarding status - tracks progress through onboarding workflow
     */
    @Enumerated(EnumType.STRING)
    private OnboardingStatus onboardingStatus;
    
    // ========== Banking Model Fields ==========
    
    /**
     * True if this location uses a shared payment account (chain model)
     * False if each location has its own payment account (franchise model)
     */
    private Boolean usesSharedPaymentAccount;
    
    /**
     * Points to the vendor ID of the location that owns the shared payment account
     * Typically the headquarters location
     * Null if usesSharedPaymentAccount is false
     */
    private Long sharedPaymentAccountVendorId;
    
    /**
     * For VENDOR workers: Links worker to their assigned location
     * Null for VENDOR_ADMIN accounts
     */
    private Long assignedLocationId;

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

    public int getBypassStrikeCount() {
        return bypassStrikeCount;
    }

    public void setBypassStrikeCount(int bypassStrikeCount) {
        this.bypassStrikeCount = bypassStrikeCount;
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

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getAboutBusiness() {
		return aboutBusiness;
	}

	public void setAboutBusiness(String aboutBusiness) {
		this.aboutBusiness = aboutBusiness;
	}

	public String getContactPerson() {
		return contactPerson;
	}

	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}

    public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getStripeAccountId() {
		return stripeAccountId;
	}

	public void setStripeAccountId(String stripeAccountId) {
		this.stripeAccountId = stripeAccountId;
	}
	
	public PayoutModel getPayoutModel() {
		return payoutModel;
	}
	
	public void setPayoutModel(PayoutModel payoutModel) {
		this.payoutModel = payoutModel;
	}
	
	public PaymentProvider getPaymentProvider() {
		return paymentProvider;
	}
	
	public void setPaymentProvider(PaymentProvider paymentProvider) {
		this.paymentProvider = paymentProvider;
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	public Boolean getStripeSupported() {
		return stripeSupported;
	}
	
	public void setStripeSupported(Boolean stripeSupported) {
		this.stripeSupported = stripeSupported;
	}
	
	public BigDecimal getBalance() {
		return balance;
	}
	
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
	
	public ManualPayoutMethod getManualPayoutMethod() {
		return manualPayoutMethod;
	}
	
	public void setManualPayoutMethod(ManualPayoutMethod manualPayoutMethod) {
		this.manualPayoutMethod = manualPayoutMethod;
	}
	
	public String getBankAccountHolderName() {
		return bankAccountHolderName;
	}
	
	public void setBankAccountHolderName(String bankAccountHolderName) {
		this.bankAccountHolderName = bankAccountHolderName;
	}
	
	public String getBankAccountNumber() {
		return bankAccountNumber;
	}
	
	public void setBankAccountNumber(String bankAccountNumber) {
		this.bankAccountNumber = bankAccountNumber;
	}
	
	public String getBankName() {
		return bankName;
	}
	
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	
	public String getBankSwiftCode() {
		return bankSwiftCode;
	}
	
	public void setBankSwiftCode(String bankSwiftCode) {
		this.bankSwiftCode = bankSwiftCode;
	}
	
	public String getBankIban() {
		return bankIban;
	}
	
	public void setBankIban(String bankIban) {
		this.bankIban = bankIban;
	}
	
	// ========== Chain and Onboarding Getters and Setters ==========
	
	public String getChainId() {
		return chainId;
	}
	
	public void setChainId(String chainId) {
		this.chainId = chainId;
	}
	
	public String getChainName() {
		return chainName;
	}
	
	public void setChainName(String chainName) {
		this.chainName = chainName;
	}
	
	public String getLocationName() {
		return locationName;
	}
	
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	
	public Boolean getIsChainLocation() {
		return isChainLocation;
	}
	
	public void setIsChainLocation(Boolean isChainLocation) {
		this.isChainLocation = isChainLocation;
	}
	
	public Boolean getIsHeadquarters() {
		return isHeadquarters;
	}
	
	public void setIsHeadquarters(Boolean isHeadquarters) {
		this.isHeadquarters = isHeadquarters;
	}
	
	public Boolean getIsUniqueVendor() {
		return isUniqueVendor;
	}
	
	public void setIsUniqueVendor(Boolean isUniqueVendor) {
		this.isUniqueVendor = isUniqueVendor;
	}
	
	public String getBusinessRegistrationId() {
		return businessRegistrationId;
	}
	
	public void setBusinessRegistrationId(String businessRegistrationId) {
		this.businessRegistrationId = businessRegistrationId;
	}
	
	public OnboardingStatus getOnboardingStatus() {
		return onboardingStatus;
	}
	
	public void setOnboardingStatus(OnboardingStatus onboardingStatus) {
		this.onboardingStatus = onboardingStatus;
	}
	
	// ========== Banking Model Getters and Setters ==========
	
	public Boolean getUsesSharedPaymentAccount() {
		return usesSharedPaymentAccount;
	}
	
	public void setUsesSharedPaymentAccount(Boolean usesSharedPaymentAccount) {
		this.usesSharedPaymentAccount = usesSharedPaymentAccount;
	}
	
	public Long getSharedPaymentAccountVendorId() {
		return sharedPaymentAccountVendorId;
	}
	
	public void setSharedPaymentAccountVendorId(Long sharedPaymentAccountVendorId) {
		this.sharedPaymentAccountVendorId = sharedPaymentAccountVendorId;
	}
	
	public Long getAssignedLocationId() {
		return assignedLocationId;
	}
	
	public void setAssignedLocationId(Long assignedLocationId) {
		this.assignedLocationId = assignedLocationId;
	}

	// ========== Legal Acceptance Getters and Setters ==========

	public LocalDateTime getTermsAcceptedAt() {
		return termsAcceptedAt;
	}

	public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) {
		this.termsAcceptedAt = termsAcceptedAt;
	}

	public String getTermsVersion() {
		return termsVersion;
	}

	public void setTermsVersion(String termsVersion) {
		this.termsVersion = termsVersion;
	}

	public LocalDateTime getPrivacyAcceptedAt() {
		return privacyAcceptedAt;
	}

	public void setPrivacyAcceptedAt(LocalDateTime privacyAcceptedAt) {
		this.privacyAcceptedAt = privacyAcceptedAt;
	}

	public String getPrivacyVersion() {
		return privacyVersion;
	}

	public void setPrivacyVersion(String privacyVersion) {
		this.privacyVersion = privacyVersion;
	}
}

