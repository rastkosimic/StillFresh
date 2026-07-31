package com.stillfresh.app.paymentservice.repository;

import com.stillfresh.app.paymentservice.model.LedgerEntry;
import com.stillfresh.app.sharedentities.enums.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByVendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);

    List<LedgerEntry> findByVendorIdAndEntryTypeAndSettledFalse(Long vendorId, LedgerEntryType entryType);

    List<LedgerEntry> findByVendorIdAndEntryTypeInAndSettledFalse(Long vendorId, List<LedgerEntryType> entryTypes);

    /**
     * Sum of unsettled credits (VENDOR_CREDIT plus PAYOUT_REVERSAL restored after
     * a rejected bank transfer) for the given vendor, in minor currency units.
     */
    @Query("SELECT COALESCE(SUM(e.amountCents), 0) FROM LedgerEntry e " +
           "WHERE e.vendorId = :vendorId AND e.entryType IN ('VENDOR_CREDIT', 'PAYOUT_REVERSAL') AND e.settled = false")
    Long sumUnsettledCreditsForVendor(@Param("vendorId") Long vendorId);

    /** Returns distinct vendor IDs that have at least one unsettled credit entry. */
    @Query("SELECT DISTINCT e.vendorId FROM LedgerEntry e " +
           "WHERE e.entryType IN ('VENDOR_CREDIT', 'PAYOUT_REVERSAL') AND e.settled = false AND e.vendorId IS NOT NULL")
    List<Long> findVendorIdsWithUnsettledCredits();

    List<LedgerEntry> findByPayoutBatchId(Long payoutBatchId);
}
