package com.stillfresh.app.vendorservice.security;

import com.stillfresh.app.vendorservice.model.Vendor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomVendorDetails implements UserDetails {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Vendor vendor;

    public CustomVendorDetails(Vendor vendor) {
        this.vendor = vendor;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + vendor.getRole().name()));
    }

    @Override
    public String getPassword() {
        return vendor.getPassword();  // Get vendor password
    }

    @Override
    public String getUsername() {
        return vendor.getUsername();
    }
    
    public String getEmail() {
        return vendor.getEmail();  
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return vendor.isActive();  // Check if vendor account is active
    }

    public Vendor getVendor() {
        return vendor;  // Additional access to the vendor entity
    }
}

