package com.stillfresh.app.paymentservice.cmiplus;

import com.stillfresh.app.sharedentities.logging.LogSanitizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Fetches intraday (CAMT052) or end-of-day (CAMT053) account statements from
 * CMIplus for payout reconciliation.
 */
@Component
public class CmiplusAccountStatementClient {

    private static final Logger logger = LoggerFactory.getLogger(CmiplusAccountStatementClient.class);

    private final CmiplusProperties properties;
    private final CmiplusHttpClient httpClient;

    public CmiplusAccountStatementClient(CmiplusProperties properties, CmiplusHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public String fetchIntradayStatement(String accountIban) {
        if (properties.isStubMode()) {
            logger.debug("[CMIplus-STUB] Skipping CAMT052 fetch for {}", LogSanitizer.maskIban(accountIban));
            return "";
        }
        String today = LocalDate.now().toString();
        return httpClient.getAccountStatement(
                properties.getAccountStatementPath(), accountIban, today, today);
    }
}
