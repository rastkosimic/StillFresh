package com.stillfresh.app.paymentservice.cmiplus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HTTP client for RBI CMIplus Open APIs with OAuth2 client-credentials token
 * caching. mTLS keystore wiring is deferred until Corporate Seal credentials
 * are available; until then {@link CmiplusProperties#isStubMode()} avoids
 * outbound calls entirely.
 */
@Component
public class CmiplusHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(CmiplusHttpClient.class);

    private final CmiplusProperties properties;
    private final RestTemplate restTemplate;
    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

    public CmiplusHttpClient(CmiplusProperties properties) {
        this.properties = properties;
        // TODO: configure mTLS RestTemplate from keystore when Corporate Seal cert is provisioned
        this.restTemplate = new RestTemplate();
    }

    public String postXml(String path, String pain001Xml) {
        String token = obtainAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(pain001Xml, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                properties.getBaseUrl() + path, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CmiplusApiException("Payment initiation failed: HTTP " + response.getStatusCode());
        }
        return response.getBody();
    }

    public String getStatus(String path, String bankMessageId) {
        String token = obtainAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = properties.getBaseUrl() + path + "?messageId=" + bankMessageId;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CmiplusApiException("Payment status request failed: HTTP " + response.getStatusCode());
        }
        return response.getBody();
    }

    public String getAccountStatement(String path, String accountIban, String fromDate, String toDate) {
        String token = obtainAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = properties.getBaseUrl() + path
                + "?iban=" + accountIban + "&from=" + fromDate + "&to=" + toDate;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CmiplusApiException("Account statement request failed: HTTP " + response.getStatusCode());
        }
        return response.getBody();
    }

    private String obtainAccessToken() {
        CachedToken cached = tokenCache.get();
        if (cached != null && cached.expiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return cached.token;
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                properties.getBaseUrl() + properties.getTokenPath(), entity, Map.class);
        if (response == null || !response.containsKey("access_token")) {
            throw new CmiplusApiException("OAuth token response missing access_token");
        }
        String token = (String) response.get("access_token");
        int expiresIn = response.containsKey("expires_in")
                ? ((Number) response.get("expires_in")).intValue() : 3600;
        tokenCache.set(new CachedToken(token, Instant.now().plusSeconds(expiresIn)));
        logger.debug("Obtained CMIplus OAuth token, expires in {}s", expiresIn);
        return token;
    }

    private record CachedToken(String token, Instant expiresAt) {}
}
