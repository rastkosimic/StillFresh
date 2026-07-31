package com.stillfresh.app.sharedentities.order.events;

import java.time.OffsetDateTime;

/**
 * Published when the user should be reminded to pick up the order (e.g. 1 hour before pickup deadline).
 */
public class OrderPickupReminderEvent {
    private String orderId;
    private String userId;
    private Long offerId;
    private OffsetDateTime pickupBy;  // Deadline by which order must be picked up
    private String offerName;  // Optional, for notification message
    // Offer/vendor display fields for notifications (optional)
    private String locationName;
    private String chainName;
    private String website;
    private String vendorImageUrl;
    private String address;
    private String zipCode;
    private String name;     // offer name
    private String imageUrl; // offer image URL (distinct from vendorImageUrl)

    public OrderPickupReminderEvent() {}

    public OrderPickupReminderEvent(String orderId, String userId, Long offerId,
                                    OffsetDateTime pickupBy, String offerName) {
        this(orderId, userId, offerId, pickupBy, offerName, null, null, null, null, null, null, null, null);
    }

    public OrderPickupReminderEvent(String orderId, String userId, Long offerId,
                                    OffsetDateTime pickupBy, String offerName,
                                    String locationName, String chainName, String website, String vendorImageUrl,
                                    String address, String zipCode, String name, String imageUrl) {
        this.orderId = orderId;
        this.userId = userId;
        this.offerId = offerId;
        this.pickupBy = pickupBy;
        this.offerName = offerName;
        this.locationName = locationName;
        this.chainName = chainName;
        this.website = website;
        this.vendorImageUrl = vendorImageUrl;
        this.address = address;
        this.zipCode = zipCode;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public OffsetDateTime getPickupBy() {
        return pickupBy;
    }

    public void setPickupBy(OffsetDateTime pickupBy) {
        this.pickupBy = pickupBy;
    }

    public String getOfferName() {
        return offerName;
    }

    public void setOfferName(String offerName) {
        this.offerName = offerName;
    }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getChainName() { return chainName; }
    public void setChainName(String chainName) { this.chainName = chainName; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getVendorImageUrl() { return vendorImageUrl; }
    public void setVendorImageUrl(String vendorImageUrl) { this.vendorImageUrl = vendorImageUrl; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
