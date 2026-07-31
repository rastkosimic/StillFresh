package com.stillfresh.app.sharedentities.order.events;

public class OrderPlacedEvent {
	private String orderId;
	private String userId;
	private Long offerId;
	private int quantity;
	private double totalPrice;
	// Offer/vendor display fields for notifications (optional; set when offer is available)
	private String locationName;
	private String chainName;
	private String website;
	private String vendorImageUrl;
	private String address;
	private String zipCode;
	private String name;       // offer name
	private String imageUrl;   // offer image URL (distinct from vendorImageUrl)
	private String customerEmail; // for transactional emails (reservation nudge)

	public OrderPlacedEvent() {}
	
	public OrderPlacedEvent(String orderId, String userId, Long offerId, int quantity, double totalPrice) {
		this(orderId, userId, offerId, quantity, totalPrice, null, null, null, null, null, null, null, null);
	}

	public OrderPlacedEvent(String orderId, String userId, Long offerId, int quantity, double totalPrice,
			String locationName, String chainName, String website, String vendorImageUrl,
			String address, String zipCode, String name, String imageUrl) {
		this.orderId = orderId;
		this.userId = userId;
		this.offerId = offerId;
		this.quantity = quantity;
		this.totalPrice = totalPrice;
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
	public String getCustomerEmail() { return customerEmail; }
	public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
}
