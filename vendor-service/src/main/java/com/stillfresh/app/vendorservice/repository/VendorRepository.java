package com.stillfresh.app.vendorservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.stillfresh.app.sharedentities.enums.PayoutModel;
import com.stillfresh.app.sharedentities.enums.Role;
import com.stillfresh.app.vendorservice.model.Vendor;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByEmail(String email);
    Optional<Vendor> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByRole(Role role);
    
    // Admin methods - find vendors by payout model
    List<Vendor> findByPayoutModel(PayoutModel payoutModel);
    
    // Chain and onboarding queries
    List<Vendor> findByChainId(String chainId);
    List<Vendor> findByChainName(String chainName);
    List<Vendor> findByOnboardingStatus(com.stillfresh.app.sharedentities.enums.OnboardingStatus status);
    List<Vendor> findByIsChainLocationTrue();
    Optional<Vendor> findByChainIdAndLocationName(String chainId, String locationName);
    List<Vendor> findByAssignedLocationId(Long locationId);
}
