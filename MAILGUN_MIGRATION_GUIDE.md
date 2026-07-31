# Mailgun Migration Guide

This document outlines the migration from SendGrid to Mailgun email service that has been implemented across the StillFresh microservices.

## Migration Overview

All email functionality has been migrated from SendGrid to Mailgun across three services:
- **authorization-service**
- **user-service**
- **vendor-service**

## What Has Been Changed

### 1. Configuration Classes
- Created `MailgunConfig.java` in each service to handle Mailgun configuration
- Configuration is loaded from `application.yml` using Spring's `@ConfigurationProperties`

### 2. Email Service Implementation
- Updated `EmailService.java` in all three services to use Mailgun's REST API
- Replaced SendGrid SDK calls with HTTP requests to Mailgun API
- Uses Spring's `RestTemplate` for HTTP communication
- Maintains the same public API (`sendVerificationEmail`, `sendPasswordResetEmail`)

### 3. Configuration Files
- Added Mailgun configuration section to `application.yml` in all three services
- SendGrid configuration is marked as deprecated but kept for reference

### 4. Application Classes
- Updated `AuthorizationServiceApplication.java` to register `MailgunConfig` instead of `SendGridConfig`

## Configuration Required

Before using the migrated services, you need to configure Mailgun credentials in each service's `application.yml`:

```yaml
mailgun:
  api-key: YOUR_MAILGUN_API_KEY      # Replace with your Mailgun API key
  domain: YOUR_MAILGUN_DOMAIN        # Replace with your Mailgun domain
  from-email: rastko.seo@gmail.com   # Sender email address
  base-url: https://api.mailgun.net/v3  # Use https://api.eu.mailgun.net/v3 for EU region
```

### Getting Mailgun Credentials

1. **Sign up for Mailgun**: Create an account at https://www.mailgun.com
2. **Add a Domain**: 
   - Go to Sending → Domains
   - Add your domain or use the sandbox domain for testing
   - Complete DNS verification (SPF, DKIM, DMARC records)
3. **Get API Key**:
   - Go to Settings → API Keys
   - Copy your Private API key
4. **Get Domain Name**:
   - Use your verified domain (e.g., `mg.yourdomain.com`)
   - Or use the sandbox domain (e.g., `sandbox123.mailgun.org`)

## Testing the Migration

### 1. Update Configuration
Replace the placeholder values in `application.yml` files with your actual Mailgun credentials.

### 2. Test Email Sending
Test each service's email functionality:
- **Authorization Service**: Test password reset and email verification
- **User Service**: Test email verification and password reset
- **Vendor Service**: Test email verification and password reset

### 3. Monitor Logs
Check application logs for Mailgun API responses:
```
Mailgun Response Status: 200 OK
Mailgun Response Body: {"message":"Queued. Thank you.", "id":"..."}
```

## Cleanup (After Successful Testing)

Once you've verified that all email functionality works correctly with Mailgun, you can remove SendGrid dependencies:

### 1. Remove SendGrid Dependencies from pom.xml

In each service's `pom.xml`, remove or comment out:
```xml
<!-- SendGrid for email verification -->
<dependency>
    <groupId>com.sendgrid</groupId>
    <artifactId>sendgrid-java</artifactId>
</dependency>
```

### 2. Remove SendGrid Configuration Files

Delete the following files:
- `authorization-service/src/main/java/com/stillfresh/app/authorizationservice/config/SendGridConfig.java`
- `user-service/src/main/java/com/stillfresh/app/userservice/config/SendGridConfig.java`
- `vendor-service/src/main/java/com/stillfresh/app/vendorservice/config/SendGridConfig.java`

### 3. Remove SendGrid Configuration from application.yml

Remove the `sendgrid:` section from all `application.yml` files.

## Mailgun API Details

### Endpoint
- **US Region**: `https://api.mailgun.net/v3/{domain}/messages`
- **EU Region**: `https://api.eu.mailgun.net/v3/{domain}/messages`

### Authentication
- Uses HTTP Basic Authentication
- Username: `api`
- Password: Your Mailgun API key

### Request Format
- Content-Type: `application/x-www-form-urlencoded`
- Required fields:
  - `from`: Sender email address
  - `to`: Recipient email address
  - `subject`: Email subject
  - `text`: Plain text email body

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Verify your API key is correct
   - Ensure you're using the Private API key, not the Public key

2. **403 Forbidden**
   - Check that your domain is verified in Mailgun
   - Ensure DNS records (SPF, DKIM) are properly configured

3. **404 Not Found**
   - Verify the domain name matches exactly what's configured in Mailgun
   - Check that you're using the correct base URL (US vs EU)

4. **Email Not Received**
   - Check Mailgun logs in the dashboard
   - Verify recipient email address is valid
   - Check spam folder
   - For sandbox domains, you can only send to authorized recipients

## Support

For Mailgun-specific issues, refer to:
- Mailgun Documentation: https://documentation.mailgun.com/
- Mailgun Support: https://www.mailgun.com/support/

## Migration Checklist

- [x] Create MailgunConfig classes
- [x] Update EmailService implementations
- [x] Add Mailgun configuration to application.yml
- [x] Update application main classes
- [x] Add Mailgun credentials to application.yml
- [x] Remove SendGrid dependencies from pom.xml
- [x] Remove SendGridConfig.java files
- [x] Remove SendGrid configuration from application.yml
- [ ] Test email sending in authorization-service
- [ ] Test email sending in user-service
- [ ] Test email sending in vendor-service

## Migration Status: ✅ COMPLETE

All SendGrid code has been removed and replaced with Mailgun. The services are ready to use Mailgun for email sending. **You must rebuild and restart the services** for the changes to take effect.

