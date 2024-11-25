package com.stillfresh.app.sharedentities.shared.events;

public class TokenRequestEvent {
	private String token;
	private String correlationId;
	
	public TokenRequestEvent() {
	}
	
	public TokenRequestEvent(String token, String correlationId) {
		this.token = token;
		this.correlationId = correlationId;
	}
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getCorrelationId() {
		return correlationId;
	}
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

}
