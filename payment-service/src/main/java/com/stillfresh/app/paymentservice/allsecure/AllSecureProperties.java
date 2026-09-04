package com.stillfresh.app.paymentservice.allsecure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the AllSecure Exchange Platform (XML Schema V2).
 *
 * <p>During testing the destination host is {@code asxgw.paymentsandbox.cloud} instead of the
 * production {@code asxgw.com}. The {@code sharedSecret} and {@code password} are secrets and must
 * be provided via environment variables.</p>
 */
@Component
@ConfigurationProperties(prefix = "allsecure")
public class AllSecureProperties {

    /** Base URL of the gateway, e.g. https://asxgw.paymentsandbox.cloud */
    private String baseUrl = "https://asxgw.paymentsandbox.cloud";

    /** Path of the transaction endpoint (also used as the request URI in the signature). */
    private String transactionPath = "/transaction";

    /** Path of the status endpoint. */
    private String statusPath = "/status";

    /** Connector API key, sent in the Authorization header (Gateway &lt;apiKey&gt;:&lt;signature&gt;). */
    private String apiKey;

    /** Connector shared secret, used as the HMAC-SHA512 key for request signing and callback verification. */
    private String sharedSecret;

    /** API username, placed in the request XML. */
    private String username;

    /** API password (plaintext); placed in the request XML SHA-1 hashed. */
    private String password;

    /** Public integration key for client-side tokenization (payment.js / mobile SDK). Unused in the hosted flow. */
    private String integrationKey;

    /** ISO 4217 currency code used for transactions (e.g. RSD). */
    private String currency = "RSD";

    /** Publicly reachable base URL used to build callbackUrl/successUrl/errorUrl. */
    private String publicBaseUrl = "http://localhost:8086";

    /**
     * Maximum number of attempts for a rate-limited request (HTTP 429 / gateway code 1009) before
     * giving up. A value of 1 disables retrying. A rate-limited request is rejected before processing,
     * so retrying the same transactionId cannot double-charge.
     */
    private int maxRetries = 4;

    /** Base backoff in milliseconds for the exponential backoff between rate-limit retries. */
    private long retryBackoffMs = 500;

    /** Upper bound (cap) in milliseconds for the backoff between rate-limit retries. */
    private long retryMaxBackoffMs = 4000;

    /** Connect timeout for outbound gateway calls, in milliseconds. */
    private int connectTimeoutMs = 5000;

    /** Read timeout for outbound gateway calls, in milliseconds. */
    private int readTimeoutMs = 20000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getTransactionPath() { return transactionPath; }
    public void setTransactionPath(String transactionPath) { this.transactionPath = transactionPath; }

    public String getStatusPath() { return statusPath; }
    public void setStatusPath(String statusPath) { this.statusPath = statusPath; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getSharedSecret() { return sharedSecret; }
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getIntegrationKey() { return integrationKey; }
    public void setIntegrationKey(String integrationKey) { this.integrationKey = integrationKey; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }

    public long getRetryMaxBackoffMs() { return retryMaxBackoffMs; }
    public void setRetryMaxBackoffMs(long retryMaxBackoffMs) { this.retryMaxBackoffMs = retryMaxBackoffMs; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    /** Full URL of the transaction endpoint. */
    public String transactionUrl() {
        return trimTrailingSlash(baseUrl) + transactionPath;
    }

    /** Full URL of the status endpoint. */
    public String statusUrl() {
        return trimTrailingSlash(baseUrl) + statusPath;
    }

    /**
     * XML namespace root the gateway expects, derived from the gateway host with an {@code http://} scheme
     * (e.g. {@code http://asxgw.paymentsandbox.cloud} for the sandbox). The official client uses the same
     * {@code 'http://' + host} convention; using the production {@code asxgw.com} namespace against the sandbox
     * yields validation error 1005.
     */
    public String namespaceRoot() {
        String host = baseUrl;
        int scheme = host.indexOf("://");
        if (scheme >= 0) {
            host = host.substring(scheme + 3);
        }
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        return "http://" + host;
    }

    /** Full transaction XML namespace, e.g. http://asxgw.paymentsandbox.cloud/Schema/V2/Transaction */
    public String transactionNamespace() {
        return namespaceRoot() + "/Schema/V2/Transaction";
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
