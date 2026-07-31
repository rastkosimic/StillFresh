package com.stillfresh.app.vendorservice.service;

import com.stillfresh.app.vendorservice.config.MailgunConfig;

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

    public void sendPasswordResetEmail(String to, String token) throws IOException {
    	String resetUrl = "http://localhost:8083/vendors/reset-password?token=" + token;
        sendEmail(to, "Password Reset", "Click the link to reset your password: " + resetUrl);
    }
    
    public void sendVendorCredentialsEmail(String to, String username, String password, String loginUrl) throws IOException {
        String subject = "Your Vendor Account Credentials - StillFresh";
        String body = String.format(
            "Welcome to StillFresh!\n\n" +
            "Your vendor account has been verified and activated.\n\n" +
            "Login Credentials:\n" +
            "Username: %s\n" +
            "Password: %s\n\n" +
            "Please log in at: %s\n\n" +
            "For security reasons, please change your password after your first login.\n\n" +
            "You will be guided through the onboarding process to set up your business profile.\n\n" +
            "Best regards,\n" +
            "StillFresh Team",
            username, password, loginUrl
        );
        sendEmail(to, subject, body);
    }

    public void sendEmail(String to, String subject, String body) throws IOException {
        try {
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
