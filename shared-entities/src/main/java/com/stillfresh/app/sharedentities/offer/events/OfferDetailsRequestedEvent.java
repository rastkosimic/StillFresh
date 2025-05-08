package com.stillfresh.app.sharedentities.offer.events;

public class OfferDetailsRequestedEvent {

	private String requestId;
	private Long offerId;
	
	public OfferDetailsRequestedEvent() {}


	public OfferDetailsRequestedEvent(String requestId, Long offerId) {
		super();
		this.requestId = requestId;
		this.offerId = offerId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Long getOfferId() {
		return offerId;
	}

	public void setOfferId(Long offerId) {
		this.offerId = offerId;
	}

}
