package com.stillfresh.app.sharedentities.vendor.events;

public class LoggedVendorEvent {
	
	private String username;
	private String email;
	
	public LoggedVendorEvent() {
	}
	
	public LoggedVendorEvent(String username, String email) {
		this.username = username;
		this.email = email;
	}
	public String getUsername() {
		return username;
	}
	public String getEmail() {
		return email;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	public void setEmail(String email) {
		this.email = email;
	}

}
