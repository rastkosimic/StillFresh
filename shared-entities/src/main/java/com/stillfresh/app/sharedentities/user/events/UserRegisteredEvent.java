package com.stillfresh.app.sharedentities.user.events;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.interfaces.Account;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public class UserRegisteredEvent implements Account{
	
	private String username;
	private String email;
	private String password;
	
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Enumerated(EnumType.STRING)
    private Role role;  // Role field

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(String email, String password, Status status, Role role, String username) {
    	super();
    	this.email = email;
    	this.password = password;
    	this.status = status;
    	this.role = role;
    	this.username = username;
    }


	public void setEmail(String email) {
		this.email = email;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
    public void setStatus(Status status) {
    	this.status = status;
    }

	public void setRole(Role role) {
		this.role = role;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Role getRole() {
		return role;
	}

	@Override	
	public boolean isActive() {
	    return this.status == Status.ACTIVE;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public Status getStatus() {
		return status;
	}
}

