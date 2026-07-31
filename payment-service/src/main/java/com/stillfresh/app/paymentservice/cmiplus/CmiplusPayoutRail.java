package com.stillfresh.app.paymentservice.cmiplus;

import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.service.executor.PayoutTransferRequest;
import com.stillfresh.app.paymentservice.service.rail.PayoutRail;
import com.stillfresh.app.paymentservice.service.rail.PayoutSubmissionResult;
import com.stillfresh.app.paymentservice.service.rail.PayoutStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Raiffeisen CMIplus Open API payout rail. Uses pain.001 submission and
 * pain.002 polling. Falls back to an internal stub when
 * {@code payout.cmiplus.stub-mode=true} (default until marketplace credentials
 * are provisioned).
 */
@Component
@ConditionalOnProperty(name = "payout.rail", havingValue = "cmiplus")
public class CmiplusPayoutRail implements PayoutRail {

    private static final Logger logger = LoggerFactory.getLogger(CmiplusPayoutRail.class);

    private final CmiplusProperties properties;
    private final CmiplusPaymentInitiationClient initiationClient;
    private final CmiplusPaymentStatusClient statusClient;

    public CmiplusPayoutRail(CmiplusProperties properties,
                             CmiplusPaymentInitiationClient initiationClient,
                             CmiplusPaymentStatusClient statusClient) {
        this.properties = properties;
        this.initiationClient = initiationClient;
        this.statusClient = statusClient;
    }

    @Override
    public PayoutSubmissionResult submit(PayoutTransferRequest request) {
        try {
            CmiplusPaymentInitiationClient.InitiationResponse response =
                    initiationClient.initiate(request);
            return PayoutSubmissionResult.submitted(
                    response.bankMessageId(), response.externalReference());
        } catch (CmiplusApiException e) {
            logger.error("[CMIplus] Submission failed for {}: {}", request.getIdempotencyKey(), e.getMessage());
            return PayoutSubmissionResult.failed(e.getMessage());
        } catch (Exception e) {
            logger.error("[CMIplus] Submission error for {}", request.getIdempotencyKey(), e);
            return PayoutSubmissionResult.failed(e.getMessage());
        }
    }

    @Override
    public PayoutStatusUpdate pollStatus(VendorPayoutItem item) {
        if (item.getBankMessageId() == null) {
            return PayoutStatusUpdate.failed("Missing bank message ID for polling");
        }
        if (properties.isStubMode()) {
            long submittedEpoch = item.getSubmittedAt() != null
                    ? item.getSubmittedAt().getEpochSecond() : 0;
            return statusClient.getStubStatus(submittedEpoch);
        }
        return statusClient.getStatus(item.getBankMessageId());
    }

    @Override
    public String railType() {
        return "CMIPLUS";
    }
}
