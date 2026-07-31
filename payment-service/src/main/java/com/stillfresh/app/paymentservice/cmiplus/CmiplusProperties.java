package com.stillfresh.app.paymentservice.cmiplus;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payout.cmiplus")
public class CmiplusProperties {

    /** sandbox | production */
    private String environment = "sandbox";
    private String baseUrl = "";
    private String clientId = "";
    private String clientSecret = "";
    private String keystorePath = "";
    private String keystorePassword = "";
    private String debtorIban = "";
    private String debtorName = "";
    /** OAuth2 token endpoint path relative to baseUrl (from marketplace OpenAPI). */
    private String tokenPath = "/oauth/token";
    /** Payment initiation endpoint path (from marketplace OpenAPI). */
    private String paymentInitiationPath = "/payment-initiation";
    /** Payment status endpoint path (from marketplace OpenAPI). */
    private String paymentStatusPath = "/payment-status";
    /** Account statement endpoint path (from marketplace OpenAPI). */
    private String accountStatementPath = "/account-statement";
    /**
     * When true, no real HTTP calls are made; submissions return SUBMITTED and
     * polling completes after {@link #stubCompleteDelaySeconds}.
     */
    private boolean stubMode = true;
    private int stubCompleteDelaySeconds = 30;

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getKeystorePath() { return keystorePath; }
    public void setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; }

    public String getKeystorePassword() { return keystorePassword; }
    public void setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; }

    public String getDebtorIban() { return debtorIban; }
    public void setDebtorIban(String debtorIban) { this.debtorIban = debtorIban; }

    public String getDebtorName() { return debtorName; }
    public void setDebtorName(String debtorName) { this.debtorName = debtorName; }

    public String getTokenPath() { return tokenPath; }
    public void setTokenPath(String tokenPath) { this.tokenPath = tokenPath; }

    public String getPaymentInitiationPath() { return paymentInitiationPath; }
    public void setPaymentInitiationPath(String paymentInitiationPath) { this.paymentInitiationPath = paymentInitiationPath; }

    public String getPaymentStatusPath() { return paymentStatusPath; }
    public void setPaymentStatusPath(String paymentStatusPath) { this.paymentStatusPath = paymentStatusPath; }

    public String getAccountStatementPath() { return accountStatementPath; }
    public void setAccountStatementPath(String accountStatementPath) { this.accountStatementPath = accountStatementPath; }

    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }

    public int getStubCompleteDelaySeconds() { return stubCompleteDelaySeconds; }
    public void setStubCompleteDelaySeconds(int stubCompleteDelaySeconds) { this.stubCompleteDelaySeconds = stubCompleteDelaySeconds; }

    public boolean isConfiguredForLiveApi() {
        return !stubMode
                && baseUrl != null && !baseUrl.isBlank()
                && clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
