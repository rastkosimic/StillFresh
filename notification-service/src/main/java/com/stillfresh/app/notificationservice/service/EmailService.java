package com.stillfresh.app.notificationservice.service;

import com.stillfresh.app.sharedentities.logging.LogSanitizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Transactional email sender backed by Mailgun's HTTP API. Disabled by default
 * ({@code notification.email.enabled=false}); when disabled, calls are no-ops so the rest of the
 * notification pipeline (push) is unaffected. All failures are swallowed/logged - email must never
 * break the Kafka consumer.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.mailgun.api-key:}")
    private String apiKey;

    @Value("${notification.email.mailgun.domain:}")
    private String domain;

    @Value("${notification.email.mailgun.from-email:}")
    private String fromEmail;

    @Value("${notification.email.mailgun.base-url:https://api.mailgun.net/v3}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isEnabled() {
        return emailEnabled;
    }

    /**
     * Sends a plain-text email. No-op when email is disabled, the recipient is missing, or Mailgun
     * configuration is incomplete.
     */
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            logger.debug("Email disabled; skipping send");
            return;
        }
        if (to == null || to.isBlank()) {
            logger.debug("No recipient; skipping email send");
            return;
        }
        if (apiKey.isBlank() || domain.isBlank() || fromEmail.isBlank()) {
            logger.warn("Email enabled but Mailgun config incomplete (api-key/domain/from-email); skipping send");
            return;
        }
        try {
            String url = baseUrl + "/" + domain + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("api", apiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("from", fromEmail);
            form.add("to", to);
            form.add("subject", subject);
            form.add("text", body);

            restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            logger.info("Sent email to {} (subject='{}')", LogSanitizer.maskEmail(to), subject);
        } catch (Exception e) {
            logger.error("Failed to send email to {} (subject='{}')", LogSanitizer.maskEmail(to), subject, e);
        }
    }
}
