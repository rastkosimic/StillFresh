package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.PaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Config-driven fiscal receipt issuer. Disabled by default ({@code fiscal.enabled=false}); when
 * disabled it only logs intent so no external dependency is required in dev/test.
 *
 * <p>When enabled, it POSTs a minimal JSON payload to a configured POS/fiscalization endpoint. The
 * exact contract (auth, payload schema, tax labels) depends on the certified POS provider and is
 * not finalized yet.</p>
 *
 * TODO: replace the placeholder payload/headers with the real certified Serbian e-fiskalni POS
 * contract (item lines, tax rates, cashier/location ids, signing) once the provider is selected.
 */
@Service
public class HttpFiscalReceiptService implements FiscalReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(HttpFiscalReceiptService.class);

    @Value("${fiscal.enabled:false}")
    private boolean fiscalEnabled;

    @Value("${fiscal.endpoint-url:}")
    private String endpointUrl;

    @Value("${fiscal.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void issueReceipt(PaymentTransaction tx) {
        if (tx == null) {
            return;
        }
        if (!fiscalEnabled) {
            logger.info("Fiscalization disabled; would issue '{}' receipt for paymentIntentId={} (gross={} {})",
                    SALE_VIA_INTERMEDIARY, tx.getPaymentIntentId(), tx.getGrossAmountCents(), tx.getCurrency());
            return;
        }
        if (endpointUrl == null || endpointUrl.isBlank()) {
            logger.warn("Fiscalization enabled but fiscal.endpoint-url is not configured; skipping receipt for paymentIntentId={}",
                    tx.getPaymentIntentId());
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("paymentReference", tx.getPaymentIntentId());
            payload.put("saleType", SALE_VIA_INTERMEDIARY);
            payload.put("grossAmountCents", tx.getGrossAmountCents());
            payload.put("currency", tx.getCurrency());
            payload.put("vendorId", tx.getVendorId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }

            restTemplate.postForEntity(endpointUrl, new HttpEntity<>(payload, headers), String.class);
            logger.info("Issued '{}' fiscal receipt for paymentIntentId={}", SALE_VIA_INTERMEDIARY, tx.getPaymentIntentId());
        } catch (Exception e) {
            logger.error("Failed to issue fiscal receipt for paymentIntentId={} (settlement continues)",
                    tx.getPaymentIntentId(), e);
        }
    }
}
