package com.stillfresh.app.sharedentities.payment.events;

import com.stillfresh.app.sharedentities.enums.Currency;

public class PaymentRequestEvent {
    private Long userId;
    private String username;
    private Long amount;
    private Long offerId;
    private String requestId;
    private Currency currency;

    public PaymentRequestEvent() {}

    public PaymentRequestEvent(Long userId, String username, Long amount, Long offerId, String requestId, Currency currency) {
    	this.setUsername(username);
        this.userId = userId;
        this.amount = amount;
        this.offerId = offerId;
        this.requestId = requestId;
        this.currency = currency;
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
}
