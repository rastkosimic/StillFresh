package com.stillfresh.app.vendorservice.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request body for a vendor editing their own profile via {@code PUT /vendors/update-profile}.
 *
 * <p>This exists specifically so the endpoint cannot bind server-controlled fields. Binding the
 * {@code Vendor} entity directly let a caller supply {@code role} and {@code status} and have them
 * persisted, which allowed any verified vendor (who holds {@code VENDOR_ADMIN}) to promote itself
 * to {@code SUPER_ADMIN}. It also allowed a vendor to set its own {@code averageRating} and
 * {@code reviewsCount}, and to overwrite bank details, onboarding status and payout model without
 * going through the flows that validate them.
 *
 * <p>Only the fields below may be self-edited. Role and status changes go through the admin
 * endpoints; bank and payout details go through the onboarding and payout endpoints.
 *
 * <p>All fields are optional: a null value leaves the stored value unchanged.
 */
public class VendorProfileUpdateRequest {

    private String username;

    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    private String address;
    private String phone;
    private String businessType;
    private List<String> operatingHours;
    private List<String> surplusFoodDetails;
    private String pricingInfo;
    private String environmentalCertifications;
    private String imageUrl;
    private String website;
    private String aboutBusiness;
    private String contactPerson;
    private String zipCode;
    private Double latitude;
    private Double longitude;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
