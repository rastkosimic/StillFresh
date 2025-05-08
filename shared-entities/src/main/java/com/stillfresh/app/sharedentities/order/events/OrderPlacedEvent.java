package com.stillfresh.app.sharedentities.order.events;

public class OrderPlacedEvent {
	private String orderId;
	private String userId;
	private int offerId;
	private int quantity;
	private double totalPrice;

	public OrderPlacedEvent() {}
	
	public OrderPlacedEvent(String orderId, String userId, int offerId, int quantity, double totalPrice) {
		super();
		this.orderId = orderId;
		this.userId = userId;
		this.offerId = offerId;
		this.quantity = quantity;
		this.totalPrice = totalPrice;
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

	public int getOfferId() {
		return offerId;
	}

	public void setOfferId(int offerId) {
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

}
