package com.stillfresh.app.sharedentities.payment.events;

public class UpdatePaymentServiceEvent {

	private String newUsername;
	private String oldUsername;

	public UpdatePaymentServiceEvent() {
	}
	
	
	
	public UpdatePaymentServiceEvent(String oldUsername, String newUsername) {
		super();
		this.newUsername = newUsername;
		this.oldUsername = oldUsername;
	}

	public String getNewUsername() {
		return newUsername;
	}

	public void setNewUsername(String newUsername) {
		this.newUsername = newUsername;
	}

	public String getOldUsername() {
		return oldUsername;
	}

	public void setOldUsername(String oldUsername) {
		this.oldUsername = oldUsername;
	}



	
}
