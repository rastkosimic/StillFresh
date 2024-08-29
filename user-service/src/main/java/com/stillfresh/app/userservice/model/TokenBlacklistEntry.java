package com.stillfresh.app.userservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "token_blacklist")
public class TokenBlacklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private Date blacklistedAt;

    // Constructors
    public TokenBlacklistEntry() {
    }

    public TokenBlacklistEntry(String token, Date blacklistedAt) {
        this.token = token;
        this.blacklistedAt = blacklistedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Date blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }
}
