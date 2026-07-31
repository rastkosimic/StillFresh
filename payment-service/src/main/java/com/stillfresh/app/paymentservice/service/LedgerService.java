package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.LedgerEntry;
import com.stillfresh.app.paymentservice.model.PaymentTransaction;
import com.stillfresh.app.paymentservice.repository.LedgerEntryRepository;
import com.stillfresh.app.paymentservice.repository.PaymentTransactionRepository;
import com.stillfresh.app.sharedentities.enums.LedgerEntryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LedgerService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private FiscalReceiptService fiscalReceiptService;

    /**
     * Writes two ledger entries for a captured payment:
     * <ol>
     *   <li>VENDOR_CREDIT — net amount owed to the vendor</li>
     *   <li>PLATFORM_FEE_INCOME — platform fee earned by StillFresh</li>
     * </ol>
     * Idempotent: no-op if the PaymentTransaction is already flagged as ledger-written.
     */
    @Transactional
    public void writePaymentLedger(PaymentTransaction tx) {
        if (tx.isLedgerWritten()) {
            logger.warn("Ledger already written for paymentIntentId: {}. Skipping.", tx.getPaymentIntentId());
            return;
        }

        if (tx.getVendorId() != null && tx.getNetAmountCents() > 0) {
            LedgerEntry vendorCredit = new LedgerEntry();
            vendorCredit.setVendorId(tx.getVendorId());
            vendorCredit.setEntryType(LedgerEntryType.VENDOR_CREDIT);
            vendorCredit.setAmountCents(tx.getNetAmountCents());
            vendorCredit.setCurrency(tx.getCurrency());
            vendorCredit.setPaymentIntentId(tx.getPaymentIntentId());
            vendorCredit.setDescription("Sale credit for payment " + tx.getPaymentIntentId());
            ledgerEntryRepository.save(vendorCredit);
        }

        if (tx.getPlatformFeeCents() > 0) {
            LedgerEntry platformFee = new LedgerEntry();
            platformFee.setVendorId(null);
            platformFee.setEntryType(LedgerEntryType.PLATFORM_FEE_INCOME);
            platformFee.setAmountCents(tx.getPlatformFeeCents());
            platformFee.setCurrency(tx.getCurrency());
            platformFee.setPaymentIntentId(tx.getPaymentIntentId());
            platformFee.setDescription("Platform fee for payment " + tx.getPaymentIntentId());
            platformFee.setSettled(true); // Platform fee is immediately "settled" into our account
            ledgerEntryRepository.save(platformFee);
        }

        tx.setLedgerWritten(true);
        paymentTransactionRepository.save(tx);

        logger.info("Ledger written for paymentIntentId={}: vendor_credit={} {}, platform_fee={} {}",
                    tx.getPaymentIntentId(),
                    tx.getNetAmountCents(), tx.getCurrency(),
                    tx.getPlatformFeeCents(), tx.getCurrency());

        // Best-effort Serbian e-fiskalni receipt ("Prodaja preko posrednika"); no-op when disabled.
        fiscalReceiptService.issueReceipt(tx);
    }

    /**
     * Writes ledger entries for a confirmed bank transfer payment.
     * Persists a synthetic PaymentTransaction (using the bank transfer reference as
     * the paymentIntentId) so the same idempotency guarantee applies.
     */
    @Transactional
    public void writeBankTransferLedger(Long vendorId, Long offerId, Long userId,
                                        String paymentReference,
                                        Long grossAmountCents, Long platformFeeCents,
                                        Long netAmountCents, String currency) {
        String syntheticRequestId = "BT-" + paymentReference;
        // Idempotency: check if we already processed this reference
        if (paymentTransactionRepository.findByRequestId(syntheticRequestId).isPresent()) {
            logger.warn("Ledger already written for bank transfer reference: {}. Skipping.", paymentReference);
            return;
        }
        PaymentTransaction tx = new PaymentTransaction();
        tx.setRequestId(syntheticRequestId);
        tx.setPaymentIntentId(paymentReference);
        tx.setUserId(userId);
        tx.setVendorId(vendorId);
        tx.setOfferId(offerId);
        tx.setGrossAmountCents(grossAmountCents);
        tx.setPlatformFeeCents(platformFeeCents);
        tx.setNetAmountCents(netAmountCents);
        tx.setCurrency(currency);
        tx = paymentTransactionRepository.save(tx);
        writePaymentLedger(tx);
    }

    /**
     * Returns the total unsettled balance owed to a vendor, in minor currency units.
     */
    @Transactional(readOnly = true)
    public long getUnsettledBalance(Long vendorId) {
        return ledgerEntryRepository.sumUnsettledCreditsForVendor(vendorId);
    }

    /**
     * Paginated ledger history for a vendor.
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntry> getVendorLedger(Long vendorId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return ledgerEntryRepository.findByVendorIdOrderByCreatedAtDesc(vendorId, pageable);
    }

    /**
     * Returns IDs of all vendors that have at least one unsettled VENDOR_CREDIT entry.
     */
    @Transactional(readOnly = true)
    public List<Long> getVendorIdsWithUnsettledCredits() {
        return ledgerEntryRepository.findVendorIdsWithUnsettledCredits();
    }

    /**
     * Marks all unsettled VENDOR_CREDIT entries for a vendor as settled and links them to the given
     * payout batch. Returns the total amount that was settled.
     */
    @Transactional
    public long settleVendorCredits(Long vendorId, Long payoutBatchId) {
        List<LedgerEntry> unsettled = ledgerEntryRepository
                .findByVendorIdAndEntryTypeAndSettledFalse(vendorId, LedgerEntryType.VENDOR_CREDIT);

        long total = 0L;
        for (LedgerEntry entry : unsettled) {
            entry.setSettled(true);
            entry.setPayoutBatchId(payoutBatchId);
            total += entry.getAmountCents();
        }
        if (!unsettled.isEmpty()) {
            ledgerEntryRepository.saveAll(unsettled);
        }

        // Write a PAYOUT_DEBIT entry to mirror the debit on the vendor balance
        if (total > 0) {
            LedgerEntry debit = new LedgerEntry();
            debit.setVendorId(vendorId);
            debit.setEntryType(LedgerEntryType.PAYOUT_DEBIT);
            debit.setAmountCents(total);
            // Currency is taken from the first settled entry; multi-currency is handled in Step 2
            debit.setCurrency(unsettled.isEmpty() ? "RSD" : unsettled.get(0).getCurrency());
            debit.setPayoutBatchId(payoutBatchId);
            debit.setSettled(true);
            debit.setDescription("Payout debit for batch " + payoutBatchId);
            ledgerEntryRepository.save(debit);
        }

        return total;
    }

    /**
     * Restores a vendor's unsettled balance after a bank transfer was rejected
     * post-submission. Writes an unsettled {@link LedgerEntryType#PAYOUT_REVERSAL}
     * entry so {@link #getUnsettledBalance} reflects the restored amount.
     */
    @Transactional
    public void reversePayoutDebit(Long vendorId, Long payoutBatchId, Long payoutItemId,
                                   long amountCents, String currency) {
        if (amountCents <= 0) {
            return;
        }
        LedgerEntry reversal = new LedgerEntry();
        reversal.setVendorId(vendorId);
        reversal.setEntryType(LedgerEntryType.PAYOUT_REVERSAL);
        reversal.setAmountCents(amountCents);
        reversal.setCurrency(currency != null ? currency : "RSD");
        reversal.setPayoutBatchId(payoutBatchId);
        reversal.setSettled(false);
        reversal.setDescription("Payout reversal for item " + payoutItemId + " in batch " + payoutBatchId);
        ledgerEntryRepository.save(reversal);
        logger.info("Reversed payout debit for vendor {} item {} batch {}: {} {}",
                vendorId, payoutItemId, payoutBatchId, amountCents, reversal.getCurrency());
    }
}
