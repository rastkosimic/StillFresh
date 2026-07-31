package com.stillfresh.app.paymentservice.allsecure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    private final AllSecureProperties properties;
    private final AllSecureSignatureService signatureService;
    private final RestTemplate restTemplate;

    public AllSecureClient(AllSecureProperties properties, AllSecureSignatureService signatureService) {
        this.properties = properties;
        this.signatureService = signatureService;
        this.restTemplate = new RestTemplate();
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

    private AllSecureResult send(String xml, String type, String transactionId) {
        String url = properties.transactionUrl();
        String requestUri = properties.getTransactionPath();
        String timestamp = signatureService.currentTimestamp();
        String signature = signatureService.sign(
                "POST", xml, CONTENT_TYPE, timestamp, requestUri, properties.getSharedSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        headers.set(HttpHeaders.DATE, timestamp);
        headers.set(HttpHeaders.AUTHORIZATION, "Gateway " + properties.getApiKey() + ":" + signature);

        logger.info("Sending AllSecure {} request (transactionId={}) to {}", type, transactionId, url);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(xml, headers), String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new AllSecureException("Empty response from AllSecure for " + type + " (transactionId=" + transactionId + ")");
            }
            AllSecureResult result = AllSecureXml.parseResult(body);
            logger.info("AllSecure {} response (transactionId={}): {}", type, transactionId, result);
            return result;
        } catch (AllSecureException e) {
            throw e;
        } catch (Exception e) {
            logger.error("AllSecure {} request failed (transactionId={}): {}", type, transactionId, e.getMessage(), e);
            throw new AllSecureException("AllSecure " + type + " request failed: " + e.getMessage(), null, e);
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
