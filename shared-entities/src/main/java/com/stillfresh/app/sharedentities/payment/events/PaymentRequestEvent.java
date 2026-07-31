package com.stillfresh.app.sharedentities.payment.events;

import com.stillfresh.app.sharedentities.enums.Currency;

public class PaymentRequestEvent {
    private Long userId;
    private String username;
    private Long amount;
    private Long offerId;
    private String requestId;
    private Currency currency;
    private Long vendorId;
    private String stripeAccountId;

    public PaymentRequestEvent() {}

    public PaymentRequestEvent(Long userId, String username, Long amount, Long offerId, String requestId, Currency currency) {
    	this.setUsername(username);
        this.userId = userId;
        this.amount = amount;
        this.offerId = offerId;
        this.requestId = requestId;
        this.currency = currency;
    }

    public PaymentRequestEvent(Long userId, String username, Long amount, Long offerId, String requestId, Currency currency, Long vendorId, String stripeAccountId) {
    	this.setUsername(username);
        this.userId = userId;
        this.amount = amount;
        this.offerId = offerId;
        this.requestId = requestId;
        this.currency = currency;
        this.vendorId = vendorId;
        this.stripeAccountId = stripeAccountId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getVendorId() {
		return vendorId;
	}

	public void setVendorId(Long vendorId) {
		this.vendorId = vendorId;
	}

	public String getStripeAccountId() {
		return stripeAccountId;
	}

	public void setStripeAccountId(String stripeAccountId) {
		this.stripeAccountId = stripeAccountId;
	}
}
