package com.stillfresh.app.sharedentities.offer.events;

public class OfferRequestEvent {
	private String requestId;
    private double latitude;
    private double longitude;
    private double range;

    public OfferRequestEvent() {}
    
    public OfferRequestEvent(String requestId, double latitude, double longitude, double range) {
    	this.requestId = requestId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.range = range;
    }

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

}

