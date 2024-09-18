package com.stillfresh.app.vendorservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stillfresh.app.vendorservice.model.Vendor;
import com.stillfresh.app.vendorservice.model.Vendor.Role;
import com.stillfresh.app.vendorservice.repository.VendorRepository;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Vendor registerVendor(Vendor vendor) {
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email");
        }
        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setActive(true);  // Activate by default
        vendor.setRole(Role.VENDOR);  // Assign VENDOR role by default
        return vendorRepository.save(vendor);
    }

    public Vendor registerAdmin(Vendor vendor) {
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Vendor already registered with this email");
        }
        vendor.setPassword(passwordEncoder.encode(vendor.getPassword()));
        vendor.setActive(true);
        vendor.setRole(Role.ADMIN);  // Assign ADMIN role
        return vendorRepository.save(vendor);
    }

    public Vendor authenticateVendor(String email, String password) {
        Vendor vendor = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        if (!passwordEncoder.matches(password, vendor.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return vendor;
    }

    public Vendor getVendorById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
    }

    public void updateVendorProfile(Long id, Vendor updatedVendor) {
        Vendor existingVendor = getVendorById(id);
        existingVendor.setName(updatedVendor.getName());
        existingVendor.setAddress(updatedVendor.getAddress());
        existingVendor.setPhone(updatedVendor.getPhone());
        vendorRepository.save(existingVendor);
    }
}
