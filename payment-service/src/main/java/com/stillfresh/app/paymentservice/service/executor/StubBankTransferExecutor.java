package com.stillfresh.app.paymentservice.service.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Development / test executor. Logs the transfer details and returns a synthetic
 * confirmation reference. No real money moves.
 * <p>
 * Set {@code payout.rail=stub} (or omit the property — stub is the default).
 * <p>
 * To simulate failures in tests, set {@code payout.executor.stub.fail=true}.
 */
@Component
@ConditionalOnProperty(name = "payout.rail", havingValue = "stub", matchIfMissing = true)
public class StubBankTransferExecutor implements BankTransferExecutor {

    private static final Logger logger = LoggerFactory.getLogger(StubBankTransferExecutor.class);

    @Value("${payout.executor.stub.fail:false}")
    private boolean simulateFailure;

    @Override
    public PayoutTransferResult execute(PayoutTransferRequest request) {
        logger.info(
            "[STUB] Bank transfer — idempotencyKey={} vendorPayoutItemId={} iban={} amount={} {} description={}",
            request.getIdempotencyKey(),
            request.getVendorPayoutItemId(),
            request.getTargetIban(),
            request.getAmountCents(),
            request.getCurrency(),
            request.getDescription()
        );

        if (simulateFailure) {
            logger.warn("[STUB] Simulating failure for idempotencyKey={}", request.getIdempotencyKey());
            return PayoutTransferResult.failure("STUB_SIMULATED_FAILURE");
        }

        String syntheticRef = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        logger.info("[STUB] Transfer accepted — ref={}", syntheticRef);
        return PayoutTransferResult.success(syntheticRef);
    }
}
