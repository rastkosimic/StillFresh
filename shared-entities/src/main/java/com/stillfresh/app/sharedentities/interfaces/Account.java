package com.stillfresh.app.sharedentities.interfaces;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.sharedentities.enums.Status;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public interface Account {
    String getEmail();
    String getUsername();
    String getPassword();
    @Enumerated(EnumType.STRING)
    Role getRole();
    @Enumerated(EnumType.STRING)
    Status getStatus();
    boolean isActive();
}
