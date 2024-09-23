package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.repository.VendorRepository;
import com.stillfresh.app.vendorservice.security.CustomVendorDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomVendorDetailsService implements UserDetailsService {

    @Autowired
    private VendorRepository vendorRepository;

    private static final Logger logger = LoggerFactory.getLogger(CustomVendorDetailsService.class);
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Vendor vendor = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Vendor not found with email: " + email));

        logger.info("Loaded Vendor Password Hash: " + vendor.getPassword());
        return new CustomVendorDetails(vendor);
    }

}

