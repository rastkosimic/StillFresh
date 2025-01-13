package com.stillfresh.app.sharedentities.offer.events;

public class AllOffersInvalidationEvent {
	private Long id;
	
	public AllOffersInvalidationEvent() {
	}

	public AllOffersInvalidationEvent(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
