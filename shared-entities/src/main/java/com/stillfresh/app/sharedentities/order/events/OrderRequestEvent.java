package com.stillfresh.app.sharedentities.order.events;

public class OrderRequestEvent {
	private Long userId;
	private String username;
	private Long offerId;
	private int quantity;
	private String requestId;
	/**
	 * Payment method chosen by the customer.
	 * Accepted values: "STRIPE_CARD" (default), "BANK_TRANSFER".
	 */
	private String paymentMethod = "STRIPE_CARD";
	/** Customer email, threaded through for transactional emails (e.g. reservation nudge). */
	private String customerEmail;

	public OrderRequestEvent() {
	}

	public OrderRequestEvent(Long userId, String username, Long offerId, int quantity) {
		super();
		this.setUsername(username);
		this.userId = userId;
		this.offerId = offerId;
		this.quantity = quantity;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
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

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}

	// Constructors, getters, setters...
}
