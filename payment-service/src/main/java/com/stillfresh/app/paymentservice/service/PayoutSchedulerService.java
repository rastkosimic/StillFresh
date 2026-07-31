package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.PayoutBatch;
import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.repository.PayoutBatchRepository;
import com.stillfresh.app.paymentservice.repository.VendorPayoutItemRepository;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import com.stillfresh.app.sharedentities.payment.events.VendorPaymentInfoResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Runs a daily job that gathers vendor unsettled balances, creates a PayoutBatch,
 * and schedules a VendorPayoutItem per eligible vendor. Ledger credits are settled
 * at submission time (not here). When auto-payout is enabled the batch is approved
 * and submitted immediately after creation.
 */
@Service
public class PayoutSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(PayoutSchedulerService.class);

    @Autowired private LedgerService ledgerService;
    @Autowired private PaymentService paymentService;
    @Autowired private PayoutBatchRepository payoutBatchRepository;
    @Autowired private VendorPayoutItemRepository vendorPayoutItemRepository;
    @Autowired private PayoutAutoOrchestrator autoOrchestrator;

    @Value("${payout.default-currency:RSD}")
    private String defaultCurrency;

    @Scheduled(cron = "${payout.schedule.cron:0 0 2 * * *}")
    @Transactional
    public void runDailyPayoutJob() {
        logger.info("Starting daily payout job");

        List<Long> vendorIds = ledgerService.getVendorIdsWithUnsettledCredits();
        if (vendorIds.isEmpty()) {
            logger.info("No vendors with unsettled credits. Payout job complete.");
            return;
        }

        logger.info("Found {} vendor(s) with unsettled credits", vendorIds.size());

        PayoutBatch batch = new PayoutBatch();
        batch.setScheduledAt(Instant.now());
        batch.setStatus(PayoutStatus.PENDING);
        batch.setCurrency(defaultCurrency);
        batch = payoutBatchRepository.save(batch);

        long batchTotal = 0L;
        int itemCount = 0;

        for (Long vendorId : vendorIds) {
            if (!isEligibleForLedgerPayout(vendorId)) {
                continue;
            }

            long amount = ledgerService.getUnsettledBalance(vendorId);
            if (amount <= 0) continue;

            VendorPayoutItem item = new VendorPayoutItem();
            item.setBatchId(batch.getId());
            item.setVendorId(vendorId);
            item.setAmountCents(amount);
            item.setCurrency(defaultCurrency);
            item.setStatus(PayoutStatus.SCHEDULED);

            enrichWithVendorBankDetails(item);

            vendorPayoutItemRepository.save(item);

            batchTotal += amount;
            itemCount++;

            logger.info("Scheduled payout for vendor {}: {} {} bankDetails={}",
                    vendorId, amount, defaultCurrency,
                    item.getTargetIban() != null ? "iban" : (item.getTargetAccountNumber() != null ? "domestic" : "MISSING"));
        }

        batch.setTotalAmountCents(batchTotal);
        batch.setItemCount(itemCount);
        payoutBatchRepository.save(batch);

        logger.info("Payout batch {} created: {} vendor(s), total {} {}",
                batch.getId(), itemCount, batchTotal, defaultCurrency);

        if (itemCount > 0) {
            autoOrchestrator.processNewBatch(batch.getId());
        }
    }

    /**
     * Only MoR / manual-payout vendors are included. Stripe Connect vendors are
     * paid via Stripe and must not appear in ledger bank-transfer batches.
     */
    private boolean isEligibleForLedgerPayout(Long vendorId) {
        try {
            VendorPaymentInfoResponseEvent info = paymentService.getVendorPaymentInfo(vendorId);
            if (info == null || !info.isSuccess()) {
                logger.warn("Skipping vendor {} — could not fetch payment info", vendorId);
                return false;
            }
            String model = info.getPayoutModel();
            if (model != null && "CONNECT".equalsIgnoreCase(model)) {
                logger.debug("Skipping Stripe Connect vendor {}", vendorId);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warn("Skipping vendor {} — payment info lookup failed: {}", vendorId, e.getMessage());
            return false;
        }
    }

    private void enrichWithVendorBankDetails(VendorPayoutItem item) {
        try {
            VendorPaymentInfoResponseEvent info = paymentService.getVendorPaymentInfo(item.getVendorId());
            if (info != null && info.isSuccess()) {
                item.setTargetIban(info.getIbanNumber());
                item.setTargetBankName(info.getBankName());
                item.setTargetAccountHolder(info.getAccountHolderName());
                item.setTargetAccountNumber(info.getAccountNumber());
                item.setTargetBankCode(info.getBankCode());
            } else {
                String reason = (info != null) ? info.getErrorMessage() : "null response";
                item.setErrorMessage("Could not fetch bank details: " + reason);
                logger.warn("Could not fetch bank details for vendor {}: {}", item.getVendorId(), reason);
            }
        } catch (Exception e) {
            item.setErrorMessage("Bank detail lookup failed: " + e.getMessage());
            logger.warn("Failed to fetch bank details for vendor {}: {}", item.getVendorId(), e.getMessage());
        }
    }
}
