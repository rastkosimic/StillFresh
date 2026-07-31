package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.cmiplus.CmiplusAccountStatementClient;
import com.stillfresh.app.paymentservice.cmiplus.CmiplusProperties;
import com.stillfresh.app.paymentservice.model.VendorPayoutItem;
import com.stillfresh.app.paymentservice.repository.VendorPayoutItemRepository;
import com.stillfresh.app.sharedentities.enums.PayoutStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cross-checks completed payout items against CMIplus account statements
 * (CAMT052/053) and flags items that cannot be matched.
 */
@Service
public class CamtReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(CamtReconciliationService.class);

    @Autowired(required = false)
    private CmiplusAccountStatementClient accountStatementClient;

    @Autowired(required = false)
    private CmiplusProperties cmiplusProperties;

    @Autowired private VendorPayoutItemRepository itemRepository;

    @Value("${bank-transfer.platform.iban:}")
    private String platformIban;

    public Map<String, Object> buildReconciliationReport(int lookbackDays) {
        Instant since = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        List<VendorPayoutItem> completed = itemRepository.findByStatus(PayoutStatus.COMPLETED).stream()
                .filter(i -> i.getProcessedAt() != null && i.getProcessedAt().isAfter(since))
                .toList();

        List<Map<String, Object>> matched = new ArrayList<>();
        List<Map<String, Object>> unmatched = new ArrayList<>();
        String camtSnippet = "";

        if (accountStatementClient != null && cmiplusProperties != null
                && !cmiplusProperties.isStubMode() && platformIban != null && !platformIban.isBlank()) {
            try {
                camtSnippet = accountStatementClient.fetchIntradayStatement(platformIban);
            } catch (Exception e) {
                logger.warn("CAMT fetch failed: {}", e.getMessage());
            }
        }

        for (VendorPayoutItem item : completed) {
            Map<String, Object> row = itemRow(item);
            boolean found = camtSnippet.isBlank()
                    || camtSnippet.contains(item.getIdempotencyKey())
                    || (item.getExternalReference() != null
                        && camtSnippet.contains(item.getExternalReference()));
            if (found || camtSnippet.isBlank()) {
                row.put("reconciled", camtSnippet.isBlank() ? "skipped" : true);
                matched.add(row);
            } else {
                row.put("reconciled", false);
                unmatched.add(row);
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("lookbackDays", lookbackDays);
        report.put("completedItemCount", completed.size());
        report.put("matchedCount", matched.size());
        report.put("unmatchedCount", unmatched.size());
        report.put("matched", matched);
        report.put("unmatched", unmatched);
        report.put("camtAvailable", !camtSnippet.isBlank());
        report.put("generatedAt", Instant.now().toString());
        return report;
    }

    private Map<String, Object> itemRow(VendorPayoutItem item) {
        Map<String, Object> row = new HashMap<>();
        row.put("itemId", item.getId());
        row.put("vendorId", item.getVendorId());
        row.put("amountCents", item.getAmountCents());
        row.put("currency", item.getCurrency());
        row.put("idempotencyKey", item.getIdempotencyKey());
        row.put("externalReference", item.getExternalReference());
        row.put("processedAt", item.getProcessedAt());
        row.put("railType", item.getRailType());
        return row;
    }
}
