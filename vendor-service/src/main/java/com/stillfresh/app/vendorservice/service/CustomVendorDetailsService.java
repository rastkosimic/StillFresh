package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomVendorDetailsService implements UserDetailsService {

    @Autowired
    private VendorRepository vendorRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Vendor vendor = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Vendor not found with email: " + email));
        return new CustomVendorDetails(vendor);
    }
}

