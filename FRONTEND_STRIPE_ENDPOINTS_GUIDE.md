# Frontend Stripe Endpoints Integration Guide

This guide provides comprehensive documentation for integrating all Stripe Connect endpoints in the mobile app frontend.

## Table of Contents
1. [Base Configuration](#base-configuration)
2. [Authentication](#authentication)
3. [Unified Payment Endpoints](#unified-payment-endpoints)
4. [Stripe Connect Endpoints](#stripe-connect-endpoints)
5. [Error Handling](#error-handling)
6. [Workflow Examples](#workflow-examples)
7. [Response Data Models](#response-data-models)

---

## Base Configuration

**Base URL**: `https://your-api-domain.com` (or `http://localhost:8083` for development)

**API Prefix**: `/vendors` (for vendor-service endpoints)

**Content-Type**: `application/json` for all requests

---

## Authentication

All Stripe endpoints require authentication via JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

**Token Management**:
- Store token securely (e.g., SecureStore in React Native, Keychain in iOS)
- Token expires after a set period - implement refresh logic
- If token is invalid/expired (401 response), redirect user to login

---

## Unified Payment Endpoints

These endpoints work for both Stripe Connect and MoR vendors. Always check the `payoutModel` in the response to determine which flow to use.

### 1. Get Payment Account Status

**Endpoint**: `GET /vendors/payment/status`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "payoutModel": "CONNECT",  // or "MOR"
  "paymentProvider": "STRIPE",  // or "MOR"
  "isReady": true,
  "hasAccount": true,
  "message": "Your payment account is ready to receive payments."
}
```

**Response Fields**:
- `payoutModel`: `"CONNECT"` (Stripe Connect) or `"MOR"` (Merchant of Record)
- `paymentProvider`: `"STRIPE"` or `"MOR"`
- `isReady`: `boolean` - Whether account is ready to receive payments
- `hasAccount`: `boolean` - Whether payment account exists
- `message`: `string` - Human-readable status message

**Error Responses**:
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to get payment account status: <message>"}`

**Implementation Notes**:
- **First endpoint to call** when user opens payment settings
- Store `payoutModel` in app state to avoid repeated API calls
- Use `isReady` to show appropriate status indicators
- If `payoutModel === "CONNECT"`, use Stripe Connect endpoints
- If `payoutModel === "MOR"`, use MoR endpoints (see MoR integration guide)

---

### 2. Get Payment Onboarding Link

**Endpoint**: `POST /vendors/payment/onboarding-link`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "onboardingUrl": "https://connect.stripe.com/setup/s/acct_xxxxx/xxxxx",
  "message": "Onboarding link generated successfully"
}
```

**For CONNECT vendors**: Returns Stripe onboarding URL
**For MoR vendors**: Returns link to bank details form (typically `/vendors/mor/bank-details`)

**Error Responses**:
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to get onboarding link: <message>"}`

**Implementation Notes**:
- For CONNECT: Open URL in WebView for Stripe onboarding
- For MoR: Navigate to bank details form in your app
- This endpoint automatically creates accounts if they don't exist
- After onboarding completion, call `GET /vendors/payment/status` to verify readiness

---

## Stripe Connect Endpoints

These endpoints are **only available/functional for vendors using the CONNECT model** (`payoutModel === "CONNECT"`). If a MoR vendor tries to access these, they will receive an error.

### 3. Get Stripe Account Status

**Endpoint**: `GET /vendors/stripe/account-status`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "isReady": true,
  "hasAccount": true,
  "message": "Your Stripe account is ready to receive payments."
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to check account status: <message>"}`

**Implementation Notes**:
- Only call this if `payoutModel === "CONNECT"` (from payment status endpoint)
- Use to check if Stripe onboarding is complete
- Show loading state while checking
- Display appropriate UI based on `isReady` status

---

### 4. Get Stripe Onboarding Link

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
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to get onboarding link: <message>"}`

**Implementation Notes**:
- Open `onboardingUrl` in a WebView or external browser
- Set up deep link handler for return URL (see endpoint #6)
- Show progress indicator during onboarding
- After completion, verify account status with endpoint #3

**WebView Configuration** (React Native example):
```javascript
<WebView
  source={{ uri: onboardingUrl }}
  onNavigationStateChange={(navState) => {
    // Handle navigation changes
    if (navState.url.includes('stripe.com/setup/success')) {
      // Onboarding completed
      checkAccountStatus();
    }
  }}
/>
```

---

### 5. Handle Stripe Onboarding Return

**Endpoint**: `GET /vendors/stripe/return?redirect=<optional_redirect_url>`

**Headers**:
```
Authorization: Bearer <jwt_token>  // Optional - may not be authenticated
```

**Query Parameters**:
- `redirect` (optional): Frontend URL to redirect to after processing

**Response** (200 OK):
```json
{
  "success": true,
  "isReady": true,
  "hasAccount": true,
  "message": "Your Stripe account has been successfully set up and is ready to receive payments!",
  "vendorEmail": "vendor@example.com"
}
```

**Error Responses**:
- **500 Internal Server Error**: `{"error": "Error processing Stripe return: <message>"}`

**Implementation Notes**:
- This endpoint is called by Stripe after onboarding completion
- Configure Stripe return URL in your Stripe dashboard to point to this endpoint
- If user is not authenticated, response includes `"requiresAuth": true`
- After successful return, navigate user to payment dashboard or status screen
- Call `GET /vendors/stripe/account-status` to verify final status

**Deep Link Configuration**:
- Set up deep link: `yourapp://stripe/return`
- Configure Stripe return URL: `https://your-api-domain.com/vendors/stripe/return?redirect=yourapp://payment-success`

---

### 6. Handle Stripe Onboarding Refresh

**Endpoint**: `GET /vendors/stripe/refresh?redirect=<optional_redirect_url>`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters**:
- `redirect` (optional): Frontend URL to redirect to after processing

**Response** (200 OK):
```json
{
  "success": true,
  "onboardingUrl": "https://connect.stripe.com/setup/s/acct_xxxxx/xxxxx",
  "message": "A new onboarding link has been generated. Please complete the onboarding process.",
  "vendorEmail": "vendor@example.com"
}
```

**Error Responses**:
- **404 Not Found**: `{"error": "Failed to generate new onboarding link. Please contact support."}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Error processing Stripe refresh: <message>"}`

**Implementation Notes**:
- Called when Stripe requires user to refresh their onboarding session
- Automatically generates a new onboarding link
- Open the returned `onboardingUrl` in WebView
- Configure Stripe refresh URL in dashboard to point to this endpoint

---

### 7. Get Stripe Account Details

**Endpoint**: `GET /vendors/stripe/account`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "accountId": "acct_xxxxx",
  "email": "vendor@example.com",
  "businessType": "individual",
  "country": "US",
  "chargesEnabled": true,
  "payoutsEnabled": true,
  "detailsSubmitted": true,
  "capabilities": {
    "card_payments": "active",
    "transfers": "active"
  },
  "requirements": {
    "currentlyDue": [],
    "eventuallyDue": [],
    "pastDue": [],
    "pendingVerification": []
  }
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve account details: <message>"}`

**Implementation Notes**:
- Use to display detailed account information in settings/profile screen
- Check `requirements.currentlyDue` to show what's needed for verification
- Show verification status based on `detailsSubmitted` and `requirements`

---

### 8. Get Stripe Dashboard Login Link

**Endpoint**: `POST /vendors/stripe/login-link`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "loginUrl": "https://connect.stripe.com/express/xxxxx"
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to create login link: <message>"}`

**Implementation Notes**:
- Opens Stripe Express Dashboard in WebView or external browser
- Link expires after a short time (typically 5 minutes)
- Use for "View Stripe Dashboard" button in settings
- Show loading state while generating link

---

### 9. Get Stripe Payout History

**Endpoint**: `GET /vendors/stripe/payouts?limit=<number>`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters**:
- `limit` (optional): Maximum number of payouts to retrieve (default: 10, max: 100)

**Response** (200 OK):
```json
[
  {
    "id": "po_xxxxx",
    "amount": 5000,
    "currency": "usd",
    "status": "paid",
    "arrivalDate": "2024-01-15T00:00:00Z",
    "created": 1705276800,
    "description": "STRIPE PAYOUT",
    "method": "standard",
    "type": "bank_account"
  },
  {
    "id": "po_yyyyy",
    "amount": 3000,
    "currency": "usd",
    "status": "pending",
    "arrivalDate": "2024-01-20T00:00:00Z",
    "created": 1705881600,
    "description": "STRIPE PAYOUT",
    "method": "standard",
    "type": "bank_account"
  }
]
```

**Response Fields**:
- `id`: Payout ID
- `amount`: Amount in cents (divide by 100 for display)
- `currency`: Currency code (e.g., "usd", "eur")
- `status`: `"paid"`, `"pending"`, `"in_transit"`, `"canceled"`, or `"failed"`
- `arrivalDate`: ISO 8601 date when payout arrives
- `created`: Unix timestamp
- `description`: Payout description
- `method`: `"standard"` or `"instant"`
- `type`: `"bank_account"` or `"card"`

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve payouts: <message>"}`

**Implementation Notes**:
- Display in a list/table with status badges
- Format amount: `(amount / 100).toFixed(2) + " " + currency.toUpperCase()`
- Show status with color coding (green for paid, yellow for pending, red for failed)
- Implement pagination if needed (use `limit` parameter)
- Show empty state if array is empty

---

### 10. Get Specific Stripe Payout

**Endpoint**: `GET /vendors/stripe/payouts/{payoutId}`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `payoutId`: Stripe payout ID (e.g., "po_xxxxx")

**Response** (200 OK):
```json
{
  "id": "po_xxxxx",
  "amount": 5000,
  "currency": "usd",
  "status": "paid",
  "arrivalDate": "2024-01-15T00:00:00Z",
  "created": 1705276800,
  "description": "STRIPE PAYOUT",
  "method": "standard",
  "type": "bank_account",
  "failureCode": null,
  "failureMessage": null,
  "statementDescriptor": null
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve payout: <message>"}`

**Implementation Notes**:
- Use for payout detail screen
- Show all fields in a readable format
- Display failure information if `status === "failed"`

---

### 11. Get Stripe Account Balance

**Endpoint**: `GET /vendors/stripe/balance`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "available": [
    {
      "amount": 10000,
      "currency": "usd",
      "sourceTypes": {
        "card": 10000
      }
    }
  ],
  "pending": [
    {
      "amount": 5000,
      "currency": "usd",
      "sourceTypes": {
        "card": 5000
      }
    }
  ],
  "connectReserved": [
    {
      "amount": 0,
      "currency": "usd"
    }
  ]
}
```

**Response Fields**:
- `available`: Array of available balances (ready to be paid out)
- `pending`: Array of pending balances (not yet available)
- `connectReserved`: Array of reserved balances
- Each balance object contains:
  - `amount`: Amount in cents
  - `currency`: Currency code
  - `sourceTypes`: Breakdown by payment method

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve balance: <message>"}`

**Implementation Notes**:
- Display prominently in payment dashboard
- Show available balance as primary amount
- Show pending balance separately
- Format amounts: `(amount / 100).toFixed(2) + " " + currency.toUpperCase()`
- Handle multiple currencies if applicable
- Refresh balance after payouts or payments

---

### 12. Get Stripe Transaction History

**Endpoint**: `GET /vendors/stripe/transactions?limit=<number>`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters**:
- `limit` (optional): Maximum number of transactions to retrieve (default: 10, max: 100)

**Response** (200 OK):
```json
[
  {
    "id": "txn_xxxxx",
    "type": "charge",
    "amount": 2500,
    "currency": "usd",
    "status": "succeeded",
    "created": 1705276800,
    "description": "Payment for order #123",
    "fee": 75,
    "net": 2425
  },
  {
    "id": "txn_yyyyy",
    "type": "payout",
    "amount": -5000,
    "currency": "usd",
    "status": "paid",
    "created": 1705190400,
    "description": "STRIPE PAYOUT",
    "fee": 0,
    "net": -5000
  }
]
```

**Response Fields**:
- `id`: Transaction ID
- `type`: `"charge"`, `"payout"`, `"refund"`, `"adjustment"`, etc.
- `amount`: Amount in cents (positive for credits, negative for debits)
- `currency`: Currency code
- `status`: Transaction status
- `created`: Unix timestamp
- `description`: Transaction description
- `fee`: Fee amount in cents (if applicable)
- `net`: Net amount after fees

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve transactions: <message>"}`

**Implementation Notes**:
- Display in chronological order (newest first)
- Show transaction type with icons
- Format amounts with +/- indicators
- Show fees separately if applicable
- Implement pagination for large transaction lists
- Filter by type if needed (charges, payouts, refunds)

---

### 13. Get Stripe Verification Requirements

**Endpoint**: `GET /vendors/stripe/requirements`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "currentlyDue": [
    "external_account",
    "individual.verification.document"
  ],
  "eventuallyDue": [],
  "pastDue": [],
  "pendingVerification": [],
  "disabledReason": null,
  "errors": []
}
```

**Response Fields**:
- `currentlyDue`: Array of requirement IDs that need to be provided now
- `eventuallyDue`: Array of requirement IDs that will be needed later
- `pastDue`: Array of requirement IDs that are past due
- `pendingVerification`: Array of requirement IDs pending verification
- `disabledReason`: Reason account is disabled (if applicable)
- `errors`: Array of verification errors

**Common Requirement IDs**:
- `"external_account"`: Bank account needed
- `"individual.verification.document"`: Identity document needed
- `"individual.verification.additional_document"`: Additional document needed
- `"business_profile.url"`: Business website URL needed
- `"business_profile.mcc"`: Merchant category code needed

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve requirements: <message>"}`

**Implementation Notes**:
- Display requirements in a checklist format
- Show status for each requirement (pending, completed, past due)
- Link to onboarding flow if requirements are missing
- Show clear messaging about what's needed
- Update UI when requirements change

---

## Error Handling

### Standard Error Response Format

All error responses follow this format:

```json
{
  "error": "Error message describing what went wrong"
}
```

### HTTP Status Codes

- **200 OK**: Request successful
- **400 Bad Request**: Invalid request parameters or business logic error
- **401 Unauthorized**: Authentication token missing, invalid, or expired
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server-side error

### Error Handling Best Practices

1. **Token Expiration (401)**:
   ```javascript
   if (response.status === 401) {
     // Clear stored token
     await AsyncStorage.removeItem('authToken');
     // Redirect to login
     navigation.navigate('Login');
   }
   ```

2. **Network Errors**:
   ```javascript
   try {
     const response = await fetch(url, options);
     if (!response.ok) {
       const error = await response.json();
       throw new Error(error.error || 'Request failed');
     }
     return await response.json();
   } catch (error) {
     if (error.message === 'Network request failed') {
       // Show network error message
       showError('No internet connection');
     } else {
       // Show error message
       showError(error.message);
     }
   }
   ```

3. **Retry Logic**:
   - Implement retry for network failures (max 3 attempts)
   - Use exponential backoff for retries
   - Don't retry on 4xx errors (client errors)

4. **User-Friendly Messages**:
   - Map technical error messages to user-friendly text
   - Show actionable error messages (e.g., "Please complete onboarding" instead of "Account not ready")

---

## Workflow Examples

### Workflow 1: Initial Payment Setup

```javascript
// 1. Check payment status
const statusResponse = await fetch('/vendors/payment/status', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const status = await statusResponse.json();

if (status.payoutModel === 'CONNECT') {
  // 2. Check if Stripe account is ready
  const accountStatus = await fetch('/vendors/stripe/account-status', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const account = await accountStatus.json();
  
  if (!account.isReady) {
    // 3. Get onboarding link
    const onboardingResponse = await fetch('/vendors/stripe/onboarding-link', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const { onboardingUrl } = await onboardingResponse.json();
    
    // 4. Open onboarding in WebView
    openWebView(onboardingUrl);
  }
}
```

### Workflow 2: View Payment Dashboard

```javascript
// 1. Get account balance
const balanceResponse = await fetch('/vendors/stripe/balance', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const balance = await balanceResponse.json();

// 2. Get recent transactions
const transactionsResponse = await fetch('/vendors/stripe/transactions?limit=10', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const transactions = await transactionsResponse.json();

// 3. Get payout history
const payoutsResponse = await fetch('/vendors/stripe/payouts?limit=10', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const payouts = await payoutsResponse.json();

// Display in dashboard UI
```

### Workflow 3: Check Verification Status

```javascript
// 1. Get requirements
const requirementsResponse = await fetch('/vendors/stripe/requirements', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const requirements = await requirementsResponse.json();

// 2. Check if verification is needed
if (requirements.currentlyDue.length > 0) {
  // Show verification needed message
  // Link to onboarding
} else if (requirements.pendingVerification.length > 0) {
  // Show pending verification message
} else {
  // Account is fully verified
}
```

### Workflow 4: Handle Onboarding Return

```javascript
// Deep link handler: yourapp://stripe/return
const handleStripeReturn = async (returnUrl) => {
  // Extract query params
  const url = new URL(returnUrl);
  const redirect = url.searchParams.get('redirect');
  
  // Call return endpoint
  const response = await fetch(`/vendors/stripe/return?redirect=${redirect}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const result = await response.json();
  
  if (result.success && result.isReady) {
    // Show success message
    // Navigate to payment dashboard
    navigation.navigate('PaymentDashboard');
  } else {
    // Show pending message
    // Navigate to payment status screen
    navigation.navigate('PaymentStatus');
  }
};
```

---

## Response Data Models

### Payment Status Response
```typescript
interface PaymentStatusResponse {
  payoutModel: 'CONNECT' | 'MOR';
  paymentProvider: 'STRIPE' | 'MOR';
  isReady: boolean;
  hasAccount: boolean;
  message: string;
}
```

### Stripe Account Status Response
```typescript
interface StripeAccountStatusResponse {
  isReady: boolean;
  hasAccount: boolean;
  message: string;
}
```

### Stripe Account Details Response
```typescript
interface StripeAccountDetailsResponse {
  accountId: string;
  email: string;
  businessType: 'individual' | 'company';
  country: string;
  chargesEnabled: boolean;
  payoutsEnabled: boolean;
  detailsSubmitted: boolean;
  capabilities: {
    [key: string]: 'active' | 'inactive' | 'pending';
  };
  requirements: {
    currentlyDue: string[];
    eventuallyDue: string[];
    pastDue: string[];
    pendingVerification: string[];
  };
}
```

### Stripe Payout Response
```typescript
interface StripePayoutResponse {
  id: string;
  amount: number;  // in cents
  currency: string;
  status: 'paid' | 'pending' | 'in_transit' | 'canceled' | 'failed';
  arrivalDate: string;  // ISO 8601
  created: number;  // Unix timestamp
  description: string;
  method: 'standard' | 'instant';
  type: 'bank_account' | 'card';
  failureCode?: string | null;
  failureMessage?: string | null;
}
```

### Stripe Balance Response
```typescript
interface StripeBalanceResponse {
  available: Array<{
    amount: number;  // in cents
    currency: string;
    sourceTypes: {
      [key: string]: number;
    };
  }>;
  pending: Array<{
    amount: number;  // in cents
    currency: string;
    sourceTypes: {
      [key: string]: number;
    };
  }>;
  connectReserved: Array<{
    amount: number;  // in cents
    currency: string;
  }>;
}
```

### Stripe Transaction Response
```typescript
interface StripeTransactionResponse {
  id: string;
  type: 'charge' | 'payout' | 'refund' | 'adjustment';
  amount: number;  // in cents (positive for credits, negative for debits)
  currency: string;
  status: string;
  created: number;  // Unix timestamp
  description: string;
  fee: number;  // in cents
  net: number;  // in cents
}
```

### Stripe Requirements Response
```typescript
interface StripeRequirementsResponse {
  currentlyDue: string[];
  eventuallyDue: string[];
  pastDue: string[];
  pendingVerification: string[];
  disabledReason: string | null;
  errors: Array<{
    code: string;
    reason: string;
    requirement: string;
  }>;
}
```

---

## Testing Checklist

- [ ] Payment status endpoint returns correct `payoutModel`
- [ ] Onboarding link opens in WebView
- [ ] Onboarding return handler processes correctly
- [ ] Account status reflects onboarding completion
- [ ] Balance displays correctly formatted
- [ ] Payout history shows all payouts
- [ ] Transaction history displays correctly
- [ ] Requirements checklist shows missing items
- [ ] Error handling works for expired tokens
- [ ] Error handling works for network failures
- [ ] Deep links configured correctly
- [ ] All endpoints require authentication
- [ ] Loading states shown during API calls
- [ ] Empty states shown when no data

---

## Additional Notes

1. **Rate Limiting**: Be aware of API rate limits. Implement request throttling if needed.

2. **Caching**: Consider caching payment status and balance data, but refresh after important actions (onboarding, payouts).

3. **Offline Support**: Store last known balance/status for offline viewing, but show indicator that data may be stale.

4. **Security**: Never log or expose JWT tokens. Use secure storage for tokens.

5. **Deep Links**: Configure deep links for Stripe return URLs:
   - iOS: Add URL scheme in `Info.plist`
   - Android: Add intent filter in `AndroidManifest.xml`

6. **WebView Configuration**: For Stripe onboarding:
   - Enable JavaScript
   - Handle navigation events
   - Set up proper error handling
   - Handle deep link redirects

---

## Support

For issues or questions:
1. Check error messages in API responses
2. Verify authentication token is valid
3. Ensure vendor has correct `payoutModel` set
4. Check network connectivity
5. Review server logs if available

