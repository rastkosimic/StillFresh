package com.stillfresh.app.sharedentities.interfaces;

import com.stillfresh.app.sharedentities.enums.Role;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public interface Account {
    Long getId();
    String getEmail();
    String getPassword();
    @Enumerated(EnumType.STRING)
    Role getRole();
    boolean isActive();
}
