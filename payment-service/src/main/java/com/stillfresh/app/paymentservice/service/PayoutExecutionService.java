package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.PayoutBatch;
import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.repository.PayoutBatchRepository;
import com.stillfresh.app.paymentservice.repository.VendorPayoutItemRepository;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the payout batch lifecycle:
 * <pre>
 *   PENDING ──► APPROVED ──► IN_PROGRESS ──► COMPLETED
 *                                        └──► PARTIALLY_COMPLETED
 *                                        └──► FAILED
 *   (stays IN_PROGRESS while any item is SUBMITTED awaiting bank confirmation)
 * </pre>
 */
@Service
public class PayoutExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(PayoutExecutionService.class);

    @Value("${payout.max-batch-amount-cents:0}")
    private long maxBatchAmountCents;

    @Autowired private PayoutBatchRepository batchRepository;
    @Autowired private VendorPayoutItemRepository itemRepository;
    @Autowired private PayoutSubmissionService submissionService;

    @Transactional
    public PayoutBatch approveBatch(Long batchId, String approvedBy) {
        PayoutBatch batch = requireBatch(batchId);

        if (batch.getStatus() != PayoutStatus.PENDING) {
            throw new IllegalStateException(
                "Batch " + batchId + " cannot be approved from status " + batch.getStatus());
        }

        batch.setStatus(PayoutStatus.APPROVED);
        batch.setApprovedAt(Instant.now());
        batch.setApprovedBy(approvedBy);
        batch = batchRepository.save(batch);

        logger.info("Batch {} approved by {}", batchId, approvedBy);
        return batch;
    }

    public List<Map<String, Object>> dryRunBatch(Long batchId) {
        requireBatch(batchId);
        List<VendorPayoutItem> items = itemRepository.findByBatchId(batchId);
        List<Map<String, Object>> preview = new ArrayList<>();

        for (VendorPayoutItem item : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("itemId", item.getId());
            row.put("vendorId", item.getVendorId());
            row.put("amountCents", item.getAmountCents());
            row.put("currency", item.getCurrency());
            row.put("targetIban", item.getTargetIban());
            row.put("targetAccountNumber", item.getTargetAccountNumber());
            row.put("targetBankCode", item.getTargetBankCode());
            row.put("targetBankName", item.getTargetBankName());
            row.put("targetAccountHolder", item.getTargetAccountHolder());
            row.put("status", item.getStatus());
            row.put("idempotencyKey", item.getIdempotencyKey());
            row.put("missingBankDetails", !submissionService.hasValidBankDetails(item));
            preview.add(row);
        }
        return preview;
    }

    @Transactional
    public PayoutBatch executeBatch(Long batchId) {
        PayoutBatch batch = requireBatch(batchId);

        if (batch.getStatus() != PayoutStatus.APPROVED) {
            throw new IllegalStateException(
                "Batch " + batchId + " must be in APPROVED status to execute, currently: " + batch.getStatus());
        }

        if (maxBatchAmountCents > 0 && batch.getTotalAmountCents() > maxBatchAmountCents) {
            throw new IllegalStateException(
                "Batch " + batchId + " total " + batch.getTotalAmountCents() +
                " cents exceeds safety limit of " + maxBatchAmountCents + " cents.");
        }

        batch.setStatus(PayoutStatus.IN_PROGRESS);
        batchRepository.save(batch);

        List<VendorPayoutItem> items = itemRepository.findByBatchId(batchId);
        for (VendorPayoutItem item : items) {
            if (item.getStatus() == PayoutStatus.COMPLETED
                    || item.getStatus() == PayoutStatus.SUBMITTED
                    || item.getStatus() == PayoutStatus.ON_HOLD
                    || item.getStatus() == PayoutStatus.CANCELLED) {
                continue;
            }
            if (item.getStatus() == PayoutStatus.SCHEDULED || item.getStatus() == PayoutStatus.FAILED) {
                submissionService.submitItem(item);
            }
        }

        return finalizeBatch(batchId);
    }

    @Transactional
    public PayoutBatch retryFailed(Long batchId) {
        PayoutBatch batch = requireBatch(batchId);

        if (batch.getStatus() != PayoutStatus.PARTIALLY_COMPLETED
                && batch.getStatus() != PayoutStatus.FAILED) {
            throw new IllegalStateException(
                "Can only retry FAILED or PARTIALLY_COMPLETED batches, not " + batch.getStatus());
        }

        List<VendorPayoutItem> failedItems =
            itemRepository.findByBatchIdAndStatus(batchId, PayoutStatus.FAILED);

        if (failedItems.isEmpty()) {
            return batch;
        }

        batch.setStatus(PayoutStatus.IN_PROGRESS);
        batchRepository.save(batch);

        for (VendorPayoutItem item : failedItems) {
            item.setStatus(PayoutStatus.SCHEDULED);
            itemRepository.save(item);
            submissionService.submitItem(item);
        }

        return finalizeBatch(batchId);
    }

    /**
     * Recomputes batch terminal status from item states. Called after execute
     * and by the status poller when async items complete.
     */
    @Transactional
    public PayoutBatch finalizeBatch(Long batchId) {
        PayoutBatch batch = requireBatch(batchId);
        List<VendorPayoutItem> items = itemRepository.findByBatchId(batchId);

        int completed = 0;
        int failed = 0;
        int submitted = 0;
        int pending = 0;

        for (VendorPayoutItem item : items) {
            switch (item.getStatus()) {
                case COMPLETED -> completed++;
                case FAILED, CANCELLED -> failed++;
                case SUBMITTED -> submitted++;
                default -> pending++;
            }
        }

        batch.setCompletedCount(completed);
        batch.setFailedCount(failed);

        if (submitted > 0 || pending > 0) {
            batch.setStatus(PayoutStatus.IN_PROGRESS);
        } else if (failed == 0) {
            batch.setStatus(PayoutStatus.COMPLETED);
            batch.setProcessedAt(Instant.now());
        } else if (completed == 0) {
            batch.setStatus(PayoutStatus.FAILED);
            batch.setProcessedAt(Instant.now());
        } else {
            batch.setStatus(PayoutStatus.PARTIALLY_COMPLETED);
            batch.setProcessedAt(Instant.now());
        }

        batch = batchRepository.save(batch);
        logger.info("Batch {} finalized — status={} completed={} failed={} submitted={}",
                batchId, batch.getStatus(), completed, failed, submitted);
        return batch;
    }

    @Transactional
    public PayoutBatch holdBatch(Long batchId) {
        PayoutBatch batch = requireBatch(batchId);
        if (batch.getStatus() == PayoutStatus.COMPLETED || batch.getStatus() == PayoutStatus.CANCELLED) {
            throw new IllegalStateException("Cannot hold batch in status " + batch.getStatus());
        }
        batch.setStatus(PayoutStatus.ON_HOLD);
        batchRepository.save(batch);

        for (VendorPayoutItem item : itemRepository.findByBatchId(batchId)) {
            if (item.getStatus() == PayoutStatus.SCHEDULED || item.getStatus() == PayoutStatus.PENDING) {
                item.setStatus(PayoutStatus.ON_HOLD);
                itemRepository.save(item);
            }
        }
        logger.info("Batch {} placed on hold", batchId);
        return batch;
    }

    @Transactional
    public PayoutBatch releaseBatch(Long batchId) {
        PayoutBatch batch = requireBatch(batchId);
        if (batch.getStatus() != PayoutStatus.ON_HOLD) {
            throw new IllegalStateException("Batch " + batchId + " is not ON_HOLD");
        }
        batch.setStatus(PayoutStatus.PENDING);
        batchRepository.save(batch);

        for (VendorPayoutItem item : itemRepository.findByBatchId(batchId)) {
            if (item.getStatus() == PayoutStatus.ON_HOLD) {
                item.setStatus(PayoutStatus.SCHEDULED);
                itemRepository.save(item);
            }
        }
        logger.info("Batch {} released from hold", batchId);
        return batch;
    }

    @Transactional
    public PayoutBatch cancelBatch(Long batchId) {
        PayoutBatch batch = requireBatch(batchId);
        if (batch.getStatus() == PayoutStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed batch");
        }

        for (VendorPayoutItem item : itemRepository.findByBatchId(batchId)) {
            if (item.getStatus() == PayoutStatus.SUBMITTED) {
                throw new IllegalStateException(
                    "Cannot cancel batch " + batchId + ": item " + item.getId() + " is already SUBMITTED to the bank");
            }
            if (item.getStatus() != PayoutStatus.COMPLETED && item.getStatus() != PayoutStatus.CANCELLED) {
                item.setStatus(PayoutStatus.CANCELLED);
                itemRepository.save(item);
            }
        }

        batch.setStatus(PayoutStatus.CANCELLED);
        batch.setProcessedAt(Instant.now());
        batchRepository.save(batch);
        logger.info("Batch {} cancelled", batchId);
        return batch;
    }

    @Transactional
    public VendorPayoutItem holdItem(Long batchId, Long itemId) {
        requireBatch(batchId);
        VendorPayoutItem item = requireItem(itemId);
        if (item.getBatchId() != batchId) {
            throw new IllegalArgumentException("Item " + itemId + " does not belong to batch " + batchId);
        }
        if (item.getStatus() == PayoutStatus.SUBMITTED || item.getStatus() == PayoutStatus.COMPLETED) {
            throw new IllegalStateException("Cannot hold item in status " + item.getStatus());
        }
        item.setStatus(PayoutStatus.ON_HOLD);
        return itemRepository.save(item);
    }

    private PayoutBatch requireBatch(Long batchId) {
        return batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Payout batch not found: " + batchId));
    }

    private VendorPayoutItem requireItem(Long itemId) {
        return itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Payout item not found: " + itemId));
    }
}
