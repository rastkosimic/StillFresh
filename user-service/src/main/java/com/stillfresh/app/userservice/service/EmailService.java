package com.stillfresh.app.userservice.service;

import com.stillfresh.app.userservice.config.MailgunConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final MailgunConfig mailgunConfig;
    private final RestTemplate restTemplate;

    @Autowired
    public EmailService(MailgunConfig mailgunConfig) {
        this.mailgunConfig = mailgunConfig;
        this.restTemplate = new RestTemplate();
    }

    public void sendVerificationEmail(String to, String verificationUrl) throws IOException {
        sendEmail(to, "Email Verification", "Click the link to verify your email: " + verificationUrl);
    }

    public void sendPasswordResetEmail(String to, String resetUrl) throws IOException {
        sendEmail(to, "Password Reset", "Click the link to reset your password: " + resetUrl);
    }

    private void sendEmail(String to, String subject, String body) throws IOException {
        try {
            if (mailgunConfig == null || mailgunConfig.getApiKey() == null || mailgunConfig.getDomain() == null) {
                throw new IllegalStateException("Mailgun configuration is not properly initialized. Check environment variables.");
            }
            
            String url = mailgunConfig.getBaseUrl() + "/" + mailgunConfig.getDomain() + "/messages";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth("api", mailgunConfig.getApiKey());
            
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("from", mailgunConfig.getFromEmail());
            formData.add("to", to);
            formData.add("subject", subject);
            formData.add("text", body);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            
            logger.info("Mailgun email sent (status={})", response.getStatusCode());
        } catch (Exception ex) {
            logger.error("Failed to send email via Mailgun");
            throw new IOException("Failed to send email via Mailgun", ex);
        }
    }
}
