package com.stillfresh.app.vendorservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for adding a new location to a chain
 */
public class LocationRequest {
    
    @NotBlank(message = "Location name is required")
    private String locationName;
    
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    private String zipCode;
    
    @NotNull(message = "Latitude is required")
    private Double latitude;
    
    @NotNull(message = "Longitude is required")
    private Double longitude;
    
    // Optional: If not provided, will use chain's default
    private String country;
    
    // Getters and Setters
    
    public String getLocationName() {
        return locationName;
    }
    
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
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
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
}

