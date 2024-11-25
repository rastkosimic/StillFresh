package com.stillfresh.app.sharedentities.user.events;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.interfaces.Account;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public class UpdateUserProfileEvent implements Account{
	
	private String username;
	private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status;
	
	public UpdateUserProfileEvent() {
	}
	
	public UpdateUserProfileEvent(String username, String email, String password, Role role, Status status) {
		super();
		this.username = username;
		this.email = email;
		this.password = password;
		this.role = role;
		this.status = status;
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	public boolean isActive() {
	    return this.status == Status.ACTIVE;
	}


	
}