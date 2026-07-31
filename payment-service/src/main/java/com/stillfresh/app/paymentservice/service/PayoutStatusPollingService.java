package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.repository.VendorPayoutItemRepository;
import com.stillfresh.app.paymentservice.service.rail.PayoutRail;
import com.stillfresh.app.paymentservice.service.rail.PayoutStatusUpdate;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Polls the bank for SUBMITTED payout items (pain.002 via CMIplus) and
 * finalizes batches when all items reach a terminal state.
 */
@Service
public class PayoutStatusPollingService {

    private static final Logger logger = LoggerFactory.getLogger(PayoutStatusPollingService.class);

    @Autowired private VendorPayoutItemRepository itemRepository;
    @Autowired private PayoutSubmissionService submissionService;
    @Autowired private PayoutExecutionService executionService;
    @Autowired private PayoutRail payoutRail;

    @Scheduled(cron = "${payout.status-poll.cron:0 */15 * * * *}")
    @Transactional
    public void pollSubmittedItems() {
        List<VendorPayoutItem> submitted = itemRepository.findByStatus(PayoutStatus.SUBMITTED);
        if (submitted.isEmpty()) {
            return;
        }

        logger.info("Polling status for {} SUBMITTED payout item(s)", submitted.size());
        Set<Long> batchesToFinalize = new HashSet<>();

        for (VendorPayoutItem item : submitted) {
            try {
                PayoutStatusUpdate update = payoutRail.pollStatus(item);
                if (update.getOutcome() != PayoutStatusUpdate.Outcome.PENDING) {
                    submissionService.applyStatusUpdate(item, update);
                    batchesToFinalize.add(item.getBatchId());
                }
            } catch (UnsupportedOperationException e) {
                logger.debug("Rail {} does not support polling", payoutRail.railType());
                return;
            } catch (Exception e) {
                logger.warn("Failed to poll status for item {}: {}", item.getId(), e.getMessage());
            }
        }

        for (Long batchId : batchesToFinalize) {
            executionService.finalizeBatch(batchId);
        }
    }
}
