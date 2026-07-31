package com.stillfresh.app.sharedentities.offer.events;

public class OfferInvalidationEvent {

    private Long id;

    public OfferInvalidationEvent() {
    }

    public OfferInvalidationEvent(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
