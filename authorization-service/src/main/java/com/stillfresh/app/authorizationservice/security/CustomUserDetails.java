package com.stillfresh.app.authorizationservice.security;

import com.stillfresh.app.authorizationservice.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();  // Get the user's password
    }

    @Override
    public String getUsername() {
        return user.getUsername();  // Return the user's username
    }
    
    public String getEmail() {
        return user.getUsername();  // Assuming email is the username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // Modify as needed for expiration policies
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // Modify as needed for account lock policies
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Modify as needed for credential expiration policies
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();  // Check if the user is active (verified)
    }

    public User getUser() {
        return user;  // Provide access to the full user entity if needed
    }
}
