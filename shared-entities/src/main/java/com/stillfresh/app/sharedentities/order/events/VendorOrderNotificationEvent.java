package com.stillfresh.app.sharedentities.order.events;

public class VendorOrderNotificationEvent {
    private Long vendorId;
    private String orderId;
    private String userId;
    private Long offerId;
    private int quantity;
    private double totalPrice;
    private String offerName;
    // Offer/vendor display fields for notifications (optional)
    private String locationName;
    private String chainName;
    private String website;
    private String vendorImageUrl;
    private String address;
    private String zipCode;
    private String imageUrl; // offer image URL (distinct from vendorImageUrl)

    public VendorOrderNotificationEvent() {}

    public VendorOrderNotificationEvent(Long vendorId, String orderId, String userId,
                                      Long offerId, int quantity, double totalPrice, String offerName) {
        this(vendorId, orderId, userId, offerId, quantity, totalPrice, offerName, null, null, null, null, null, null, null);
    }

    public VendorOrderNotificationEvent(Long vendorId, String orderId, String userId,
                                      Long offerId, int quantity, double totalPrice, String offerName,
                                      String locationName, String chainName, String website, String vendorImageUrl,
                                      String address, String zipCode, String imageUrl) {
        this.vendorId = vendorId;
        this.orderId = orderId;
        this.userId = userId;
        this.offerId = offerId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.offerName = offerName;
        this.locationName = locationName;
        this.chainName = chainName;
        this.website = website;
        this.vendorImageUrl = vendorImageUrl;
        this.address = address;
        this.zipCode = zipCode;
        this.imageUrl = imageUrl;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
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
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public String toString() {
        return "VendorOrderNotificationEvent{" +
                "vendorId=" + vendorId +
                ", orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", offerId=" + offerId +
                ", quantity=" + quantity +
                ", totalPrice=" + totalPrice +
                ", offerName='" + offerName + '\'' +
                '}';
    }
}
