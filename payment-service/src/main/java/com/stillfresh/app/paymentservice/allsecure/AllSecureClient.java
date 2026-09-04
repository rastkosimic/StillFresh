package com.stillfresh.app.paymentservice.allsecure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Thin HTTP client for the AllSecure Exchange transaction API. Builds the XML payload, signs the
 * request (Authorization + Date headers), POSTs it, and parses the &lt;result&gt; response.
 */
@Component
public class AllSecureClient {

    private static final Logger logger = LoggerFactory.getLogger(AllSecureClient.class);

    /** Path of the callback endpoint exposed by this service (must be publicly reachable). */
    public static final String CALLBACK_PATH = "/payment/allsecure/callback";
    /** Path of the browser landing endpoint AllSecure redirects to after a hosted card entry. */
    public static final String RETURN_PATH = "/payment/allsecure/return";

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    /** Gateway error code returned when the connector's rate limit is exceeded (accompanies HTTP 429). */
    private static final String RATE_LIMIT_CODE = "1009";

    private final AllSecureProperties properties;
    private final AllSecureSignatureService signatureService;
    private final RestTemplate restTemplate;

    public AllSecureClient(AllSecureProperties properties, AllSecureSignatureService signatureService) {
        this.properties = properties;
        this.signatureService = signatureService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    public AllSecureResult register(String transactionId, String customerIdentification, String description) {
        String xml = AllSecureXml.buildRegister(
                namespace(), properties.getUsername(), hashedPassword(), transactionId, customerIdentification,
                description, returnUrl("success"), returnUrl("error"), returnUrl("cancel"), callbackUrl());
        return send(xml, "register", transactionId);
    }

    public AllSecureResult preauthorize(String transactionId, String customerIdentification, String amount,
                                        String currency, String description, String referenceTransactionId,
                                        String transactionIndicator) {
        String xml = AllSecureXml.buildPreauthorize(
                namespace(), properties.getUsername(), hashedPassword(), transactionId, customerIdentification, amount,
                currency, description, referenceTransactionId, transactionIndicator, callbackUrl());
        return send(xml, "preauthorize", transactionId);
    }

    public AllSecureResult capture(String transactionId, String referenceTransactionId, String amount, String currency) {
        String xml = AllSecureXml.buildCapture(
                namespace(), properties.getUsername(), hashedPassword(), transactionId, referenceTransactionId, amount, currency);
        return send(xml, "capture", transactionId);
    }

    public AllSecureResult voidTransaction(String transactionId, String referenceTransactionId) {
        String xml = AllSecureXml.buildVoid(
                namespace(), properties.getUsername(), hashedPassword(), transactionId, referenceTransactionId);
        return send(xml, "void", transactionId);
    }

    public AllSecureResult deregister(String transactionId, String referenceTransactionId) {
        String xml = AllSecureXml.buildDeregister(
                namespace(), properties.getUsername(), hashedPassword(), transactionId, referenceTransactionId);
        return send(xml, "deregister", transactionId);
    }

    private String namespace() {
        return properties.transactionNamespace();
    }

    /**
     * Sends a signed transaction request, transparently retrying when the gateway rate-limits us
     * (HTTP 429 / error code {@value #RATE_LIMIT_CODE}). A rate-limited request is rejected before any
     * processing takes place, so retrying the same {@code transactionId} is safe and cannot double-charge.
     * Retries use exponential backoff with jitter and honour a {@code Retry-After} header when present.
     */
    private AllSecureResult send(String xml, String type, String transactionId) {
        String url = properties.transactionUrl();
        String requestUri = properties.getTransactionPath();
        int maxAttempts = Math.max(1, properties.getMaxRetries());

        HttpClientErrorException lastRateLimit = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // Sign each attempt afresh: the signature covers the Date header, which must be current.
            String timestamp = signatureService.currentTimestamp();
            String signature = signatureService.sign(
                    "POST", xml, CONTENT_TYPE, timestamp, requestUri, properties.getSharedSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
            headers.set(HttpHeaders.DATE, timestamp);
            headers.set(HttpHeaders.AUTHORIZATION, "Gateway " + properties.getApiKey() + ":" + signature);

            logger.info("Sending AllSecure {} request (transactionId={}, attempt={}/{}) to {}",
                    type, transactionId, attempt, maxAttempts, url);
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, new HttpEntity<>(xml, headers), String.class);
                String body = response.getBody();
                if (body == null || body.isBlank()) {
                    throw new AllSecureException("Empty response from AllSecure for " + type + " (transactionId=" + transactionId + ")");
                }
                AllSecureResult result = AllSecureXml.parseResult(body);
                // Defensive: some deployments may surface the rate limit as HTTP 200 with code 1009.
                if (RATE_LIMIT_CODE.equals(result.getErrorCode()) && attempt < maxAttempts) {
                    long backoff = computeBackoffMs(attempt, null);
                    logger.warn("AllSecure {} rate limited (code {}) on attempt {}/{} (transactionId={}); retrying in {} ms",
                            type, RATE_LIMIT_CODE, attempt, maxAttempts, transactionId, backoff);
                    sleep(backoff);
                    continue;
                }
                logger.info("AllSecure {} response (transactionId={}, returnType={}, referenceId={}, errorCode={})",
                        type, transactionId, result.getReturnType(), result.getReferenceId(), result.getErrorCode());
                return result;
            } catch (HttpClientErrorException.TooManyRequests e) {
                lastRateLimit = e;
                if (attempt < maxAttempts) {
                    long backoff = computeBackoffMs(attempt, e);
                    logger.warn("AllSecure {} rate limited (HTTP 429) on attempt {}/{} (transactionId={}); retrying in {} ms",
                            type, attempt, maxAttempts, transactionId, backoff);
                    sleep(backoff);
                } else {
                    logger.error("AllSecure {} still rate limited (HTTP 429) after {} attempts (transactionId={}); giving up.",
                            type, maxAttempts, transactionId);
                }
            } catch (AllSecureException e) {
                throw e;
            } catch (Exception e) {
                logger.error("AllSecure {} request failed (transactionId={}): {}", type, transactionId, e.getMessage(), e);
                throw new AllSecureException("AllSecure " + type + " request failed: " + e.getMessage(), null, e);
            }
        }
        throw new AllSecureException(
                "AllSecure " + type + " rate limited after " + maxAttempts + " attempts (transactionId=" + transactionId + ")",
                RATE_LIMIT_CODE, lastRateLimit);
    }

    /**
     * Computes the backoff before the next retry: honours a numeric {@code Retry-After} (seconds) header
     * when the gateway provides one, otherwise uses exponential backoff with equal jitter, capped by
     * {@code allsecure.retry-max-backoff-ms}.
     */
    private long computeBackoffMs(int attempt, HttpClientErrorException e) {
        long base = properties.getRetryBackoffMs();
        long cap = properties.getRetryMaxBackoffMs();
        long exp = Math.min(cap, base * (1L << (attempt - 1)));
        long jittered = exp / 2 + ThreadLocalRandom.current().nextLong((exp / 2) + 1);
        Long retryAfter = retryAfterMs(e);
        if (retryAfter != null) {
            return Math.min(cap, Math.max(retryAfter, jittered));
        }
        return jittered;
    }

    /** Parses a numeric {@code Retry-After} header (in seconds) into milliseconds; returns null if absent/non-numeric. */
    private static Long retryAfterMs(HttpClientErrorException e) {
        if (e == null) {
            return null;
        }
        HttpHeaders responseHeaders = e.getResponseHeaders();
        if (responseHeaders == null) {
            return null;
        }
        String value = responseHeaders.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim()) * 1000L;
        } catch (NumberFormatException ignored) {
            return null; // HTTP-date form is not supported here; fall back to exponential backoff.
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AllSecureException("Interrupted while backing off before an AllSecure retry", null, ie);
        }
    }

    private String hashedPassword() {
        return signatureService.sha1Password(properties.getPassword());
    }

    private String callbackUrl() {
        return trimTrailingSlash(properties.getPublicBaseUrl()) + CALLBACK_PATH;
    }

    private String returnUrl(String status) {
        return trimTrailingSlash(properties.getPublicBaseUrl()) + RETURN_PATH + "?status=" + status;
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public AllSecureProperties getProperties() {
        return properties;
    }
}
