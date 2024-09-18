package com.stillfresh.app.vendorservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.stillfresh.app.vendorservice.model.Vendor;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByEmail(String email);
    boolean existsByEmail(String email);
}
