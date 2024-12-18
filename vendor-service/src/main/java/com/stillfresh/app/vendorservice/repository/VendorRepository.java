package com.stillfresh.app.vendorservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.vendorservice.model.Vendor;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByEmail(String email);
    Optional<Vendor> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
}
