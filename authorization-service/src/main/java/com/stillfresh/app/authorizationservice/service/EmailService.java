package com.stillfresh.app.authorizationservice.service;

import com.stillfresh.app.authorizationservice.config.MailgunConfig;

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
    
    public void sendPasswordResetVerificationEmail(String to, String verificationUrl) throws IOException {
        String subject = "Confirm Your Password Reset - StillFresh";
        String body = "You have requested to reset your password for your StillFresh account.\n\n" +
                     "Please click the link below to confirm and complete the password reset:\n\n" +
                     verificationUrl + "\n\n" +
                     "If you did not request a password reset, please ignore this email. Your password will remain unchanged.\n\n" +
                     "This link will expire in 24 hours.\n\n" +
                     "For security reasons, never share this link with anyone.\n\n" +
                     "Best regards,\n" +
                     "StillFresh Team";
        sendEmail(to, subject, body);
    }
    
    public void sendPasswordChangedNotificationEmail(String to) throws IOException {
        String subject = "Your Password Has Been Changed \u2013 StillFresh";
        String body = "Your StillFresh account password has been changed successfully.\n\n" +
                     "If you made this change, no further action is required.\n\n" +
                     "If you did not make this change, please contact our support team immediately, " +
                     "as your account may have been compromised.\n\n" +
                     "Best regards,\n" +
                     "StillFresh Team";
        sendEmail(to, subject, body);
    }

    public void sendPasswordResetConfirmationEmail(String to) throws IOException {
        String subject = "Password Reset Confirmed - StillFresh";
        String body = "Your password has been successfully reset for your StillFresh account.\n\n" +
                     "If you did not reset your password, please contact support immediately.\n\n" +
                     "For security reasons, you have been logged out of all devices. Please log in again with your new password.\n\n" +
                     "Best regards,\n" +
                     "StillFresh Team";
        sendEmail(to, subject, body);
    }

    public void sendWelcomeEmail(String to, String name) throws IOException {
        String welcomeMessage = buildWelcomeEmailBody(name);
        sendEmail(to, "Welcome to StillFresh!", welcomeMessage);
    }

    private String buildWelcomeEmailBody(String name) {
        StringBuilder body = new StringBuilder();
        body.append("Hello");
        if (name != null && !name.isEmpty()) {
            body.append(" ").append(name);
        }
        body.append(",\n\n");
        body.append("Welcome to StillFresh! We're thrilled to have you on board.\n\n");
        body.append("Thank you for signing up with Google. Your account has been successfully created and is ready to use.\n\n");
        body.append("You can now:\n");
        body.append("- Browse fresh products and offers\n");
        body.append("- Place orders and track them in real-time\n");
        body.append("- Manage your profile and preferences\n\n");
        body.append("If you have any questions or need assistance, feel free to reach out to our support team.\n\n");
        body.append("Happy shopping!\n\n");
        body.append("Best regards,\n");
        body.append("The StillFresh Team");
        return body.toString();
    }

    private void sendEmail(String to, String subject, String body) throws IOException {
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
            logger.error("Failed to send email via Mailgun (status unavailable)");
            throw new IOException("Failed to send email via Mailgun", ex);
        }
    }
}
