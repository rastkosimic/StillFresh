package com.stillfresh.app.userservice.model;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;
import com.stillfresh.app.sharedentities.interfaces.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Long id;

    @NotEmpty(message = "Username is required")
    private String username;

    @NotEmpty(message = "Password is required")
    @Size(min = 6, message = "Password should have at least 6 characters")
    private String password;

    @NotEmpty(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Status status;

    // Optional profile fields
    private String firstName;
    private String lastName;
    private String address;
    private String country;
    private LocalDate birthday;
    private String dietaryPreference;
    private String phoneNumber;

    // Anti-abuse strike counters (incremented via Kafka events from order-service).
    @Column(name = "bypass_strike_count", nullable = false)
    private int bypassStrikeCount = 0;

    @Column(name = "no_show_strike_count", nullable = false)
    private int noShowStrikeCount = 0;

    // Legal acceptance audit. The version is supplied by the client (the version of the
    // document that was displayed); the timestamp is stamped server-side at acceptance.
    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "terms_version")
    private String termsVersion;

    @Column(name = "privacy_accepted_at")
    private LocalDateTime privacyAcceptedAt;

    @Column(name = "privacy_version")
    private String privacyVersion;

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
    
    public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public boolean isActive() {
	    return this.status == Status.ACTIVE;
	}

	public int getBypassStrikeCount() {
		return bypassStrikeCount;
	}

	public void setBypassStrikeCount(int bypassStrikeCount) {
		this.bypassStrikeCount = bypassStrikeCount;
	}

	public int getNoShowStrikeCount() {
		return noShowStrikeCount;
	}

	public void setNoShowStrikeCount(int noShowStrikeCount) {
		this.noShowStrikeCount = noShowStrikeCount;
	}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getDietaryPreference() {
        return dietaryPreference;
    }

    public void setDietaryPreference(String dietaryPreference) {
        this.dietaryPreference = dietaryPreference;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getTermsAcceptedAt() {
        return termsAcceptedAt;
    }

    public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) {
        this.termsAcceptedAt = termsAcceptedAt;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }

    public LocalDateTime getPrivacyAcceptedAt() {
        return privacyAcceptedAt;
    }

    public void setPrivacyAcceptedAt(LocalDateTime privacyAcceptedAt) {
        this.privacyAcceptedAt = privacyAcceptedAt;
    }

    public String getPrivacyVersion() {
        return privacyVersion;
    }

    public void setPrivacyVersion(String privacyVersion) {
        this.privacyVersion = privacyVersion;
    }

}
