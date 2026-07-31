package com.stillfresh.app.paymentservice.cmiplus;

import com.stillfresh.app.paymentservice.service.rail.PayoutStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Requests payment status (pain.002) from CMIplus and maps ISO 20022 status
 * codes to {@link PayoutStatusUpdate}.
 */
@Component
public class CmiplusPaymentStatusClient {

    private static final Logger logger = LoggerFactory.getLogger(CmiplusPaymentStatusClient.class);

    private static final Pattern TX_STS_PATTERN =
            Pattern.compile("<TxSts>([A-Z]{4})</TxSts>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADDTL_INF_PATTERN =
            Pattern.compile("<AddtlInf>([^<]*)</AddtlInf>");

    private final CmiplusProperties properties;
    private final CmiplusHttpClient httpClient;

    public CmiplusPaymentStatusClient(CmiplusProperties properties, CmiplusHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public PayoutStatusUpdate getStatus(String bankMessageId) {
        if (properties.isStubMode()) {
            return PayoutStatusUpdate.pending();
        }

        String pain002 = httpClient.getStatus(properties.getPaymentStatusPath(), bankMessageId);
        return parsePain002(pain002);
    }

    /**
     * Stub-mode helper: after the configured delay, treat the payment as settled.
     */
    public PayoutStatusUpdate getStubStatus(long submittedAtEpochSeconds) {
        long elapsed = System.currentTimeMillis() / 1000 - submittedAtEpochSeconds;
        if (elapsed >= properties.getStubCompleteDelaySeconds()) {
            return PayoutStatusUpdate.completed("CMIplus-STUB-SETTLED");
        }
        return PayoutStatusUpdate.pending();
    }

    PayoutStatusUpdate parsePain002(String pain002) {
        if (pain002 == null || pain002.isBlank()) {
            return PayoutStatusUpdate.pending();
        }

        Matcher stsMatcher = TX_STS_PATTERN.matcher(pain002);
        if (!stsMatcher.find()) {
            logger.warn("pain.002 response missing TxSts; treating as pending");
            return PayoutStatusUpdate.pending();
        }

        String txSts = stsMatcher.group(1).toUpperCase();
        return switch (txSts) {
            case "ACSC", "ACCC", "ACCP" -> PayoutStatusUpdate.completed(txSts);
            case "RJCT" -> {
                Matcher infMatcher = ADDTL_INF_PATTERN.matcher(pain002);
                String reason = infMatcher.find() ? infMatcher.group(1) : "Bank rejected (RJCT)";
                yield PayoutStatusUpdate.failed(reason);
            }
            case "PDNG", "ACTC", "ACSP" -> PayoutStatusUpdate.pending();
            default -> {
                logger.warn("Unknown pain.002 TxSts={}; treating as pending", txSts);
                yield PayoutStatusUpdate.pending();
            }
        };
    }
}
