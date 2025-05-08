package com.stillfresh.app.sharedentities.offer.events;

import com.stillfresh.app.sharedentities.dto.OfferDto;

public class OfferDetailsResponseEvent {
    private String requestId;  // Ties the response to the original request
    private OfferDto offerDto; // Contains details of the offer

    public OfferDetailsResponseEvent() {}

    public OfferDetailsResponseEvent(String requestId, OfferDto offerDto) {
        this.requestId = requestId;
        this.offerDto = offerDto;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public OfferDto getOfferDto() {
        return offerDto;
    }

    public void setOfferDto(OfferDto offerDto) {
        this.offerDto = offerDto;
    }
}
