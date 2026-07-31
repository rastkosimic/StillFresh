package com.stillfresh.app.authorizationservice.model;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.interfaces.Account;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
}, indexes = {
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_email", columnList = "email")
})
public class User implements Account{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Username is required")
    private String username;

    // Password is optional for OAuth2 users (e.g., Google sign-in)
    private String password;
    
    // OAuth2 provider information
    private String oauth2Provider; // e.g., "GOOGLE"
    private String oauth2ProviderId; // Google user ID

    @NotEmpty(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;
    
    @Enumerated(EnumType.STRING)
    private Status status;

//    private Boolean active = false;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public Status getStatus() {
    	return status;
    }
    
    public void setStatus(Status status) {
    	this.status = status;
    }

    public boolean isActive() {
	    return this.status == Status.ACTIVE;
	}

    public String getOauth2Provider() {
        return oauth2Provider;
    }

    public void setOauth2Provider(String oauth2Provider) {
        this.oauth2Provider = oauth2Provider;
    }

    public String getOauth2ProviderId() {
        return oauth2ProviderId;
    }

    public void setOauth2ProviderId(String oauth2ProviderId) {
        this.oauth2ProviderId = oauth2ProviderId;
    }

    public boolean isOAuth2User() {
        return oauth2Provider != null && !oauth2Provider.isEmpty();
    }

}
