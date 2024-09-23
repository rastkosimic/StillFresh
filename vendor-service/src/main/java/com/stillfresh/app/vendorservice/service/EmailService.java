package com.stillfresh.app.vendorservice.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.stillfresh.app.vendorservice.config.SendGridConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private final SendGridConfig sendGridConfig;

    @Autowired
    public EmailService(SendGridConfig sendGridConfig) {
        this.sendGridConfig = sendGridConfig;
    }

    public void sendVerificationEmail(String to, String verificationUrl) throws IOException {
        sendEmail(to, "Email Verification", "Click the link to verify your email: " + verificationUrl);
    }

    public void sendPasswordResetEmail(String to, String token) throws IOException {
    	String resetUrl = "http://localhost:8083/vendors/reset-password?token=" + token;
        sendEmail(to, "Password Reset", "Click the link to reset your password: " + resetUrl);
    }

    private void sendEmail(String to, String subject, String body) throws IOException {
        Email from = new Email("rastko.seo@gmail.com");
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridConfig.getApiKey());
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            throw ex;
        }
    }
}
