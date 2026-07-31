package com.stillfresh.app.vendorservice.repository;

import com.stillfresh.app.vendorservice.model.VendorBalanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorBalanceTransactionRepository extends JpaRepository<VendorBalanceTransaction, Long> {
    
    List<VendorBalanceTransaction> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    
    List<VendorBalanceTransaction> findByVendorIdAndTypeOrderByCreatedAtDesc(Long vendorId, String type);
    
    // Admin methods - find all transactions by type
    List<VendorBalanceTransaction> findByTypeOrderByCreatedAtDesc(String type);
    
    // Admin methods - find transactions by order ID
    List<VendorBalanceTransaction> findByOrderId(Long orderId);
}

