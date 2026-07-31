package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.repository.VendorPayoutItemRepository;
import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;
import com.stillfresh.app.paymentservice.service.rail.PayoutRail;
import com.stillfresh.app.paymentservice.service.rail.PayoutSubmissionResult;
import com.stillfresh.app.paymentservice.service.rail.PayoutStatusUpdate;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Submits vendor transfers to the active {@link PayoutRail} and settles ledger
 * credits at submission time (not at batch scheduling time).
 */
@Service
public class PayoutSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(PayoutSubmissionService.class);

    @Autowired private VendorPayoutItemRepository itemRepository;
    @Autowired private LedgerService ledgerService;
    @Autowired private PayoutRail payoutRail;

    /**
     * Returns true when the vendor has enough bank detail to submit a domestic
     * or IBAN transfer (RSD: IBAN or account number + bank code).
     */
    public boolean hasValidBankDetails(VendorPayoutItem item) {
        if (item.getTargetIban() != null && !item.getTargetIban().isBlank()) {
            return true;
        }
        return item.getTargetAccountNumber() != null && !item.getTargetAccountNumber().isBlank()
                && item.getTargetBankCode() != null && !item.getTargetBankCode().isBlank();
    }

    @Transactional
    public void submitItem(VendorPayoutItem item) {
        if (item.getStatus() == PayoutStatus.ON_HOLD || item.getStatus() == PayoutStatus.CANCELLED) {
            return;
        }
        if (item.getStatus() == PayoutStatus.COMPLETED || item.getStatus() == PayoutStatus.SUBMITTED) {
            return;
        }

        if (!hasValidBankDetails(item)) {
            failItem(item, "No IBAN or domestic account (number + bank code) on file for vendor " + item.getVendorId());
            return;
        }

        item.setAttemptCount(item.getAttemptCount() + 1);
        item.setLastAttemptAt(Instant.now());
        item.setRailType(payoutRail.railType());

        long settled = ledgerService.settleVendorCredits(item.getVendorId(), item.getBatchId());
        if (settled <= 0) {
            failItem(item, "No unsettled credits to pay out for vendor " + item.getVendorId());
            return;
        }
        if (settled != item.getAmountCents()) {
            logger.warn("Item {} amount {} differs from settled {}; using settled amount",
                    item.getId(), item.getAmountCents(), settled);
            item.setAmountCents(settled);
        }

        String description = String.format("StillFresh payout — vendor %d batch %d",
                item.getVendorId(), item.getBatchId());

        PayoutTransferRequest request = new PayoutTransferRequest(
                item.getId(),
                item.getIdempotencyKey(),
                item.getTargetIban(),
                item.getTargetBankName(),
                item.getTargetAccountHolder(),
                item.getTargetAccountNumber(),
                item.getTargetBankCode(),
                item.getAmountCents(),
                item.getCurrency(),
                description
        );

        try {
            PayoutSubmissionResult result = payoutRail.submit(request);
            applySubmissionResult(item, result);
        } catch (Exception e) {
            ledgerService.reversePayoutDebit(item.getVendorId(), item.getBatchId(), item.getId(),
                    item.getAmountCents(), item.getCurrency());
            failItem(item, e.getMessage());
            logger.error("Item {} threw exception — vendorId={}", item.getId(), item.getVendorId(), e);
        }

        itemRepository.save(item);
    }

    @Transactional
    public void applyStatusUpdate(VendorPayoutItem item, PayoutStatusUpdate update) {
        switch (update.getOutcome()) {
            case PENDING -> { /* no change */ }
            case COMPLETED -> {
                item.setStatus(PayoutStatus.COMPLETED);
                if (update.getExternalReference() != null) {
                    item.setExternalReference(update.getExternalReference());
                }
                item.setProcessedAt(Instant.now());
                item.setErrorMessage(null);
            }
            case FAILED -> {
                if (item.getStatus() == PayoutStatus.SUBMITTED) {
                    ledgerService.reversePayoutDebit(item.getVendorId(), item.getBatchId(), item.getId(),
                            item.getAmountCents(), item.getCurrency());
                }
                item.setStatus(PayoutStatus.FAILED);
                item.setErrorMessage(update.getErrorMessage());
            }
        }
        itemRepository.save(item);
    }

    private void applySubmissionResult(VendorPayoutItem item, PayoutSubmissionResult result) {
        switch (result.getOutcome()) {
            case COMPLETED -> {
                item.setStatus(PayoutStatus.COMPLETED);
                item.setExternalReference(result.getExternalReference());
                item.setProcessedAt(Instant.now());
                item.setErrorMessage(null);
            }
            case SUBMITTED -> {
                item.setStatus(PayoutStatus.SUBMITTED);
                item.setBankMessageId(result.getBankMessageId());
                item.setExternalReference(result.getExternalReference());
                item.setSubmittedAt(Instant.now());
                item.setErrorMessage(null);
            }
            case FAILED -> {
                ledgerService.reversePayoutDebit(item.getVendorId(), item.getBatchId(), item.getId(),
                        item.getAmountCents(), item.getCurrency());
                failItem(item, result.getErrorMessage());
            }
        }
    }

    private void failItem(VendorPayoutItem item, String message) {
        item.setStatus(PayoutStatus.FAILED);
        item.setErrorMessage(message);
    }
}
