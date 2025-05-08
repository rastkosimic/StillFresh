package com.stillfresh.app.sharedentities.offer.events;

import java.util.List;

import com.stillfresh.app.sharedentities.dto.OfferDto;

public class AvailableOffersEvent {
	private String requestId;
    private List<OfferDto> availableOffers;

    public AvailableOffersEvent() {}
    
    public AvailableOffersEvent(String requestId, List<OfferDto> availableOffers) {
    	this.requestId = requestId;
        this.availableOffers = availableOffers;
    }

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

    public List<OfferDto> getAvailableOffers() {
        return availableOffers;
    }

    public void setAvailableOffers(List<OfferDto> availableOffers) {
        this.availableOffers = availableOffers;
    }
}

