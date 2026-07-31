package com.stillfresh.app.sharedentities.order.events;

public class OrderCancelledEvent {
    private String orderId;
    private String userId;
    private Long vendorId;
    private Long offerId;
    private int quantity;
    private double totalPrice;
    private String cancelledBy; // "CUSTOMER" or "VENDOR"
    private String reason; // Optional reason for cancellation
    // Offer/vendor display fields for notifications (optional)
    private String locationName;
    private String chainName;
    private String website;
    private String vendorImageUrl;
    private String address;
    private String zipCode;
    private String name;     // offer name
    private String imageUrl; // offer image URL (distinct from vendorImageUrl)

    public OrderCancelledEvent() {}

    public OrderCancelledEvent(String orderId, String userId, Long vendorId, Long offerId,
                              int quantity, double totalPrice, String cancelledBy, String reason) {
        this(orderId, userId, vendorId, offerId, quantity, totalPrice, cancelledBy, reason,
             null, null, null, null, null, null, null, null);
    }

    public OrderCancelledEvent(String orderId, String userId, Long vendorId, Long offerId,
                              int quantity, double totalPrice, String cancelledBy, String reason,
                              String locationName, String chainName, String website, String vendorImageUrl,
                              String address, String zipCode, String name, String imageUrl) {
        this.orderId = orderId;
        this.userId = userId;
        this.vendorId = vendorId;
        this.offerId = offerId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.cancelledBy = cancelledBy;
        this.reason = reason;
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

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
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

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    @Override
    public String toString() {
        return "OrderCancelledEvent{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", vendorId=" + vendorId +
                ", offerId=" + offerId +
                ", quantity=" + quantity +
                ", totalPrice=" + totalPrice +
                ", cancelledBy='" + cancelledBy + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
