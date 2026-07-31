package com.stillfresh.app.vendorservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for initial vendor registration by platform admins
 * Contains minimal information needed to create a pending vendor account
 */
public class PendingVendorRegistrationRequest {
    
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    private String email;
    
    @NotBlank(message = "Phone number cannot be blank")
    @Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
    private String phone;
    
    @NotBlank(message = "Business address cannot be blank")
    private String businessAddress;

    private String locationName;
    
    private String zipCode;
    
    private String businessRegistrationId; // Optional: Business registration/tax ID

    private String contactPerson;

    private Double latitude;

    private Double longitude;

    // Version of the legal documents the applicant accepted (as displayed to them).
    private String termsVersion;

    private String privacyVersion;

    // Getters and Setters
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getBusinessAddress() {
        return businessAddress;
    }
    
    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public String getBusinessRegistrationId() {
        return businessRegistrationId;
    }
    
    public void setBusinessRegistrationId(String businessRegistrationId) {
        this.businessRegistrationId = businessRegistrationId;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
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

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }

    public String getPrivacyVersion() {
        return privacyVersion;
    }

    public void setPrivacyVersion(String privacyVersion) {
        this.privacyVersion = privacyVersion;
    }
}

