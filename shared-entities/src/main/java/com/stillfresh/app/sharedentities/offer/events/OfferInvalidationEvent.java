package com.stillfresh.app.sharedentities.offer.events;

public class OfferInvalidationEvent {
	
	private int id;
	
	public OfferInvalidationEvent() {
	}

	public OfferInvalidationEvent(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


}
