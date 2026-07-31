package com.stillfresh.app.vendorservice.repository;

import com.stillfresh.app.vendorservice.model.VendorPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPayoutRepository extends JpaRepository<VendorPayout, Long> {
    
    List<VendorPayout> findByVendorIdOrderByRequestedAtDesc(Long vendorId);
    
    List<VendorPayout> findByVendorIdAndStatusOrderByRequestedAtDesc(Long vendorId, String status);
    
    // Admin methods - find all payouts by status
    List<VendorPayout> findByStatusOrderByRequestedAtDesc(String status);
    
    // Admin methods - find all payouts with multiple statuses
    List<VendorPayout> findByStatusInOrderByRequestedAtDesc(List<String> statuses);
    
    // Admin methods - find all payouts
    List<VendorPayout> findAllByOrderByRequestedAtDesc();
}

