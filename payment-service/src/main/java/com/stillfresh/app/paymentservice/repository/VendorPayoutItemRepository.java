package com.stillfresh.app.paymentservice.repository;

import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPayoutItemRepository extends JpaRepository<VendorPayoutItem, Long> {

    List<VendorPayoutItem> findByBatchId(Long batchId);

    List<VendorPayoutItem> findByBatchIdAndStatus(Long batchId, PayoutStatus status);

    Page<VendorPayoutItem> findByVendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);

    List<VendorPayoutItem> findByStatus(PayoutStatus status);

    /** Most recent completed payout for a vendor. */
    java.util.Optional<VendorPayoutItem> findTopByVendorIdAndStatusOrderByProcessedAtDesc(Long vendorId, PayoutStatus status);
}
