package com.stillfresh.app.sharedentities.offer.events;

public class OfferQuantityUpdatedEvent {

	private Long offerId;
	private int quantityChange;

	public OfferQuantityUpdatedEvent() {}
	
	public OfferQuantityUpdatedEvent(Long offerId, int quantityChange) {
		super();
		this.offerId = offerId;
		this.quantityChange = quantityChange;
	}

	public Long getOfferId() {
		return offerId;
	}

	public void setOfferId(Long offerId) {
		this.offerId = offerId;
	}

	public int getQuantityChange() {
		return quantityChange;
	}

	public void setQuantityChange(int quantityChange) {
		this.quantityChange = quantityChange;
	}

}
