# Google OAuth2 Sign-In/Sign-Up Implementation Guide

## Overview
This implementation allows customers and vendors to sign in or sign up using their Google accounts. The system supports both mobile app and web-based authentication flows.

## Architecture

### Components
1. **OAuth2Controller** - REST endpoints for OAuth2 authentication
2. **OAuth2Service** - Business logic for processing Google authentication
3. **User Model** - Extended with OAuth2 fields (`oauth2Provider`, `oauth2ProviderId`)
4. **SecurityConfig** - Updated to support OAuth2 login flow

## Setup Instructions

### 1. Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable **Google+ API** (or **Google Identity API**)
4. Go to **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID**
5. Configure OAuth consent screen:
   - User Type: External (for public use) or Internal (for organization)
   - Scopes: `email`, `profile`
6. Create OAuth 2.0 Client ID:
   - Application type: **Web application** (for web) or **iOS/Android** (for mobile)
   - Authorized redirect URIs:
     - For web: `http://localhost:8082/auth/oauth2/callback/google`
     - For production: `https://yourdomain.com/auth/oauth2/callback/google`
7. Copy the **Client ID** and **Client Secret**

### 2. Environment Configuration

Update `authorization-service/src/main/resources/application.yml` or set environment variables:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:your-google-client-id}
            client-secret: ${GOOGLE_CLIENT_SECRET:your-google-client-secret}
```

Or set environment variables:
```bash
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### 3. Database Migration

The User table has been extended with OAuth2 fields:
- `oauth2_provider` (VARCHAR) - e.g., "GOOGLE"
- `oauth2_provider_id` (VARCHAR) - Google user ID

Hibernate will automatically create these columns when the service starts (with `ddl-auto: update`).

## API Endpoints

### 1. Mobile App Flow (Recommended)

**POST** `/auth/oauth2/google/login`

Request body:
```json
{
  "idToken": "google-id-token-from-mobile-app",
  "role": "USER"  // or "VENDOR"
}
```

Response:
```json
{
  "token": "jwt-token",
  "role": "USER"
}
```

**Mobile App Implementation:**
1. Use Google Sign-In SDK to authenticate user
2. Get the ID token from Google
3. Send POST request to `/auth/oauth2/google/login` with the ID token
4. Receive JWT token and use it for subsequent API calls

### 2. Web Flow (Alternative)

**GET** `/auth/oauth2/google/customer` - Redirects to Google login for customers
**GET** `/auth/oauth2/google/vendor` - Redirects to Google login for vendors

**GET** `/auth/oauth2/callback/google?role=USER` - Callback endpoint (handled by Spring Security)

## How It Works

### Sign-Up Flow (New User)
1. User authenticates with Google (via mobile app or web)
2. Backend receives Google ID token or OAuth2 user info
3. System checks if user exists by:
   - Google provider ID (if already registered via Google)
   - Email address (if registered via email/password)
4. If user doesn't exist:
   - Generate global user ID
   - Create user account with:
     - Email from Google
     - Username generated from name or email
     - Role (USER or VENDOR)
     - Status: ACTIVE (no email verification needed)
     - OAuth2 provider info stored
5. Generate and return JWT token

### Sign-In Flow (Existing User)
1. User authenticates with Google
2. System finds user by Google provider ID or email
3. If user exists but not linked to Google:
   - Link the existing account to Google OAuth2
4. Generate and return JWT token

### Account Linking
- If a user registered with email/password and later signs in with Google (same email), the accounts are automatically linked
- User can then use either authentication method

## Features

### ✅ Automatic Account Creation
- New users are automatically registered when they sign in with Google
- No separate sign-up step required

### ✅ Account Linking
- Existing email/password accounts are linked to Google if same email is used

### ✅ Role Support
- Supports both USER (customer) and VENDOR roles
- Role is specified in the login request

### ✅ No Email Verification
- OAuth2 users are automatically activated (Google already verified their email)

### ✅ Username Generation
- Automatically generates unique username from Google name or email
- Ensures uniqueness by appending numbers if needed

## Security Considerations

1. **ID Token Validation**: The mobile app endpoint validates Google ID tokens server-side
2. **JWT Tokens**: Standard JWT tokens are issued after successful OAuth2 authentication
3. **Password Handling**: OAuth2 users have a random password stored (not used for authentication)
4. **Account Status**: OAuth2 users are automatically set to ACTIVE status

## Testing

### Test with Swagger
1. Navigate to Swagger UI: `http://localhost:8082/swagger-ui.html`
2. Find `/auth/oauth2/google/login` endpoint
3. Use a valid Google ID token (from Google Sign-In SDK)
4. Test with both USER and VENDOR roles

### Test with cURL (Mobile Flow)
```bash
curl -X POST http://localhost:8082/auth/oauth2/google/login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "your-google-id-token",
    "role": "USER"
  }'
```

### Test Web Flow
1. Navigate to: `http://localhost:8082/auth/oauth2/google/customer`
2. Complete Google authentication
3. You'll be redirected to callback with JWT token

## Integration with User/Vendor Services

**Note**: Currently, OAuth2 registration creates users only in the authorization-service. You may need to:

1. **Publish events** to create corresponding records in `user-service` or `vendor-service`
2. **Call service APIs directly** to create user/vendor profiles
3. **Handle this in a listener** that responds to user creation events

This depends on your architecture. If users/vendors need profiles in their respective services, add the integration logic.

## Troubleshooting

### Error: "Email not provided by Google"
- Ensure Google OAuth2 scopes include `email`
- Check that user granted email permission

### Error: "Invalid Google ID token"
- Verify the ID token is valid and not expired
- Check Google Client ID matches in configuration

### Error: "User already exists"
- This is expected if user registered via email/password
- Account will be automatically linked

## Next Steps

1. **Configure Google OAuth2 credentials** in environment variables
2. **Update mobile app** to use the new `/auth/oauth2/google/login` endpoint
3. **Test the flow** with both new and existing users
4. **Integrate with user-service/vendor-service** if needed for profile creation
5. **Update API Gateway routes** if necessary to expose OAuth2 endpoints

## Support

For issues or questions, check:
- Google OAuth2 documentation: https://developers.google.com/identity/protocols/oauth2
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html

