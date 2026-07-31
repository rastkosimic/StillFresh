# Mobile App AI Agent Prompt: Vendor Stripe Onboarding Integration

## Overview
You are an AI agent responsible for implementing the Stripe Connect onboarding flow for vendors in a mobile application. The backend API provides endpoints to manage vendor Stripe account setup, which is required for vendors to receive payments from customers.

## Base API Configuration
- **Base URL**: `http://localhost:8083` (development) or your production API gateway URL
- **Authentication**: JWT Bearer token required for protected endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

## Authentication Flow

### 1. Vendor Login
Before accessing Stripe onboarding endpoints, the vendor must be authenticated.

**Endpoint**: `POST /auth/login` (via authorization-service)

**Request Body**:
```json
{
  "identifier": "vendor@example.com",  // Can be email or username
  "password": "vendor_password"
}
```

**Response**:
- **200 OK**: Returns JWT token as plain string
- **401 Unauthorized**: Invalid credentials

**Implementation Notes**:
- Store the JWT token securely (e.g., Keychain/SecureStorage)
- Include token in `Authorization` header for all protected requests: `Bearer <token>`
- Token expires after 1 hour (3600000ms)

## Stripe Onboarding Workflow

### Step 1: Check Current Account Status

**Endpoint**: `GET /vendors/stripe/account-status`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "isReady": false,
  "hasAccount": true,
  "message": "Your Stripe account is not ready. Please complete onboarding."
}
```

**Response Fields**:
- `isReady` (boolean): `true` if account can receive payments, `false` otherwise
- `hasAccount` (boolean): `true` if vendor has a Stripe account ID
- `message` (string): Human-readable status message

**Implementation Notes**:
- Call this endpoint when vendor navigates to payment settings screen
- If `isReady` is `true`, show success state - no onboarding needed
- If `hasAccount` is `false`, proceed to Step 2 to create account
- If `hasAccount` is `true` but `isReady` is `false`, proceed to Step 2 to get onboarding link

### Step 2: Get Onboarding Link

**Endpoint**: `GET /vendors/stripe/onboarding-link`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "onboardingUrl": "https://connect.stripe.com/setup/s/acct_xxxxx/xxxxx"
}
```

**Error Responses**:
- **404 Not Found**: `{"error": "Stripe account not found. Please contact support."}`
- **500 Internal Server Error**: `{"error": "Failed to get onboarding link: <error_message>"}`

**Implementation Notes**:
- This endpoint automatically creates a Stripe account if one doesn't exist
- The `onboardingUrl` is a Stripe-hosted page that the vendor must complete
- Open this URL in an in-app browser (WebView) or external browser
- The onboarding process typically takes 5-10 minutes

### Step 3: Handle Stripe Return

After vendor completes Stripe onboarding, Stripe redirects to the return URL.

**Endpoint**: `GET /vendors/stripe/return?redirect=<optional_frontend_url>`

**Query Parameters**:
- `redirect` (optional): Frontend URL to redirect to after processing

**Response** (200 OK) - Authenticated:
```json
{
  "success": true,
  "isReady": true,
  "hasAccount": true,
  "message": "Your Stripe account has been successfully set up and is ready to receive payments!",
  "vendorEmail": "vendor@example.com"
}
```

**Response** (200 OK) - Not Authenticated:
```json
{
  "success": true,
  "message": "Stripe onboarding completed. Please log in to check your account status.",
  "requiresAuth": true
}
```

**Implementation Notes**:
- Configure Stripe return URL in your app's deep link handler
- If vendor is authenticated, check `isReady` status
- If `isReady` is `true`, show success screen
- If `isReady` is `false`, show "pending verification" message
- If `requiresAuth` is `true`, prompt vendor to log in

### Step 4: Handle Stripe Refresh (Optional)

If Stripe onboarding session expires, Stripe redirects to refresh URL.

**Endpoint**: `GET /vendors/stripe/refresh?redirect=<optional_frontend_url>`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "success": true,
  "onboardingUrl": "https://connect.stripe.com/setup/s/acct_xxxxx/xxxxx",
  "message": "A new onboarding link has been generated. Please complete the onboarding process.",
  "vendorEmail": "vendor@example.com"
}
```

**Implementation Notes**:
- This endpoint generates a new onboarding link
- Redirect vendor to the new `onboardingUrl`
- This is typically needed if vendor closes browser during onboarding

## Mobile App Implementation Guide

### 1. Payment Settings Screen

**UI Flow**:
```
┌─────────────────────────────┐
│  Payment Settings           │
├─────────────────────────────┤
│  [Check Status Button]      │
│                             │
│  Status: Not Set Up         │
│  [Set Up Payments]          │
└─────────────────────────────┘
```

**Implementation Steps**:
1. On screen load, call `GET /vendors/stripe/account-status`
2. Display status based on response:
   - If `isReady: true`: Show "✅ Payments Ready" with green checkmark
   - If `hasAccount: false`: Show "Set Up Payments" button
   - If `hasAccount: true` but `isReady: false`: Show "Complete Setup" button

### 2. Onboarding Flow

**When user taps "Set Up Payments" or "Complete Setup"**:

1. **Show Loading Indicator**
   - Display: "Preparing your payment setup..."

2. **Call API**: `GET /vendors/stripe/onboarding-link`
   - Handle errors gracefully
   - If 404 or 500, show error message: "Unable to start payment setup. Please try again later or contact support."

3. **Open Stripe Onboarding**
   - Extract `onboardingUrl` from response
   - Open in WebView or external browser
   - **Important**: Use a WebView that can handle redirects back to your app

4. **Monitor for Return**
   - Set up deep link handler for: `yourapp://stripe/return`
   - Or use URL scheme: `stillfresh://vendors/stripe/return`
   - When Stripe redirects, your app should intercept the URL

5. **Process Return**
   - Call `GET /vendors/stripe/return`
   - Parse response:
     - If `isReady: true`: Show success screen
     - If `isReady: false`: Show "Verification in progress" message
     - If `requiresAuth: true`: Prompt for login

### 3. Deep Link Configuration

**iOS (Info.plist)**:
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>stillfresh</string>
        </array>
    </dict>
</array>
```

**Android (AndroidManifest.xml)**:
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="stillfresh" android:host="vendors" />
</intent-filter>
```

**Deep Link URL Format**:
- Return: `stillfresh://vendors/stripe/return`
- Refresh: `stillfresh://vendors/stripe/refresh`

### 4. Error Handling

**Network Errors**:
- Show user-friendly message: "Unable to connect. Please check your internet connection."
- Provide retry button

**API Errors**:
- **401 Unauthorized**: Token expired - redirect to login
- **404 Not Found**: Account not found - show "Contact support" message
- **500 Internal Server Error**: Show generic error with retry option

**Stripe Errors**:
- If Stripe returns error during onboarding, show: "There was an issue with payment setup. Please try again."
- Allow user to retry from beginning

### 5. State Management

**Recommended State Variables**:
```typescript
interface StripeOnboardingState {
  isLoading: boolean;
  hasAccount: boolean;
  isReady: boolean;
  onboardingUrl: string | null;
  error: string | null;
  lastChecked: Date | null;
}
```

**State Transitions**:
- `initial` → `checking` → `needs_onboarding` → `onboarding` → `completed` / `pending`
- Handle `error` state at any point

### 6. User Experience Best Practices

**Loading States**:
- Show loading spinner during API calls
- Disable buttons while processing
- Provide clear feedback: "Setting up your payment account..."

**Success States**:
- Show celebration animation when `isReady: true`
- Display: "🎉 Your payment account is ready! You can now receive payments."

**Pending States**:
- If `isReady: false` after onboarding, show: "Your account is being verified. This usually takes 1-2 business days."
- Provide option to check status again
- Show last checked timestamp

**Error Recovery**:
- Always provide "Try Again" button
- For persistent errors, show "Contact Support" option
- Log errors for debugging

## Complete Flow Diagram

```
┌─────────────────┐
│ Vendor Opens    │
│ Payment Screen  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ GET /stripe/account-   │
│ status                  │
└────────┬────────────────┘
         │
    ┌────┴────┐
    │         │
isReady?  hasAccount?
    │         │
    ▼         ▼
┌────────┐ ┌──────────────────┐
│ Show   │ │ GET /stripe/      │
│ Success│ │ onboarding-link   │
└────────┘ └────────┬───────────┘
                    │
                    ▼
            ┌───────────────┐
            │ Open Stripe    │
            │ Onboarding URL │
            └───────┬───────┘
                    │
                    ▼
            ┌───────────────┐
            │ Vendor         │
            │ Completes Form │
            └───────┬───────┘
                    │
                    ▼
            ┌───────────────┐
            │ Stripe         │
            │ Redirects to   │
            │ /stripe/return │
            └───────┬───────┘
                    │
                    ▼
            ┌───────────────┐
            │ GET /stripe/   │
            │ return         │
            └───────┬───────┘
                    │
            ┌───────┴───────┐
            │               │
        isReady?        requiresAuth?
            │               │
            ▼               ▼
    ┌───────────┐   ┌──────────────┐
    │ Show      │   │ Prompt Login │
    │ Success   │   └──────────────┘
    └───────────┘
```

## Testing Checklist

- [ ] Vendor can check account status when logged in
- [ ] Onboarding link is generated successfully
- [ ] Stripe onboarding page opens correctly
- [ ] Deep link handler captures return URL
- [ ] Return endpoint processes correctly
- [ ] Success state displays when account is ready
- [ ] Pending state displays when verification in progress
- [ ] Error states handled gracefully
- [ ] Token expiration handled (redirect to login)
- [ ] Network errors handled with retry
- [ ] Refresh endpoint works if session expires

## Security Considerations

1. **Token Storage**: Store JWT in secure storage (Keychain/SecureStorage), never in UserDefaults/SharedPreferences
2. **HTTPS Only**: Always use HTTPS in production
3. **Token Refresh**: Implement token refresh before expiration
4. **Deep Link Validation**: Validate deep link URLs before processing
5. **Error Messages**: Don't expose sensitive error details to users

## Additional Notes

- Stripe onboarding typically requires: business information, bank account details, tax information, identity verification
- Account verification can take 1-2 business days after onboarding completion
- Vendor can check status anytime using `/stripe/account-status` endpoint
- If vendor closes browser during onboarding, they can request a new link using `/stripe/onboarding-link` again

## Support Contacts

If vendor encounters issues:
- Show in-app support contact option
- Provide email: support@stillfresh.com (or your support email)
- Include error code and timestamp in support requests

---

**Last Updated**: Based on API version as of implementation date
**API Base URL**: Configure based on environment (dev/staging/production)











