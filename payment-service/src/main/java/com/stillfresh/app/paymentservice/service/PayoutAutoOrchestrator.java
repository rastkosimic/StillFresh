package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.PayoutBatch;
import com.stillfresh.app.paymentservice.repository.PayoutBatchRepository;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs the automatic approve-and-execute pipeline after a batch is created,
 * unless the global auto switch is paused or the batch is on hold.
 */
@Service
public class PayoutAutoOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PayoutAutoOrchestrator.class);
    private static final String SYSTEM = "SYSTEM";

    @Value("${payout.auto.approve:true}")
    private boolean autoApprove;

    @Value("${payout.auto.execute:true}")
    private boolean autoExecute;

    @Autowired private PayoutAutoControlService autoControl;
    @Autowired private PayoutExecutionService executionService;
    @Autowired private PayoutBatchRepository batchRepository;

    public void processNewBatch(Long batchId) {
        if (!autoControl.isAutoEnabled()) {
            logger.info("Payout auto pipeline paused — batch {} awaiting manual action", batchId);
            return;
        }

        PayoutBatch batch = batchRepository.findById(batchId).orElse(null);
        if (batch == null || batch.getStatus() == PayoutStatus.ON_HOLD) {
            return;
        }

        try {
            if (autoApprove && batch.getStatus() == PayoutStatus.PENDING) {
                batch = executionService.approveBatch(batchId, SYSTEM);
            }
            if (autoExecute && batch.getStatus() == PayoutStatus.APPROVED) {
                executionService.executeBatch(batchId);
            }
        } catch (Exception e) {
            logger.error("Auto payout pipeline failed for batch {}: {}", batchId, e.getMessage(), e);
        }
    }
}
