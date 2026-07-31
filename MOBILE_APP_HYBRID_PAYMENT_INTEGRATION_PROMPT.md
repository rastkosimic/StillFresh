# Mobile App AI Agent Prompt: Hybrid Payment Integration (Stripe Connect + MoR)

## Overview
You are an AI agent responsible for implementing a hybrid payment system for vendors in a mobile application. The backend supports two payment models based on the vendor's country:

1. **Stripe Connect** (CONNECT): For vendors in Stripe-supported countries (US, EU, etc.)
   - Vendors onboard through Stripe
   - Payments split automatically via Stripe Connect
   - Vendors receive payments directly in their Stripe account

2. **Merchant of Record (MoR)**: For vendors in unsupported countries (Balkan region, etc.)
   - Platform acts as seller, vendors are suppliers
   - Payments go to platform account
   - Vendors maintain internal balance
   - Manual payouts via bank transfer

**Key Concept**: The system automatically determines which model to use based on the vendor's country. Your mobile app must handle both models seamlessly.

## Base API Configuration
- **Base URL**: `http://localhost:8083` (development) or your production API gateway URL
- **Authentication**: JWT Bearer token required for all protected endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

## Authentication Flow

### Vendor Login
Before accessing payment endpoints, the vendor must be authenticated.

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
- If token expires (401 response), redirect vendor to login screen

---

## Step 1: Determine Payment Model

Before showing any payment UI, you must determine which payment model the vendor uses.

### Get Payment Account Status

**Endpoint**: `GET /vendors/payment/status`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "isReady": true,
  "hasAccount": true,
  "provider": "MOR",
  "payoutModel": "MOR",
  "accountId": "vendor@example.com",
  "country": "RS",
  "stripeSupported": false,
  "balance": 125000,
  "manualPayoutMethod": "BANK",
  "hasBankDetails": true,
  "message": "Your payment account is ready to receive payments."
}
```

**Response Fields**:
- `isReady` (boolean): Whether account is ready to receive payments
- `hasAccount` (boolean): Whether vendor has a payment account set up
- `provider` (string): `"STRIPE"` or `"MOR"`
- `payoutModel` (string): `"CONNECT"` or `"MOR"` - **This is the key field to check**
- `accountId` (string): Stripe account ID (for CONNECT) or email (for MOR)
- `country` (string): ISO 2-letter country code (e.g., "RS", "DE", "US")
- `stripeSupported` (boolean): Whether vendor's country supports Stripe
- `balance` (number, MoR only): Current balance in cents (e.g., 125000 = €1,250.00)
- `manualPayoutMethod` (string, MoR only): `"BANK"`, `"WISE"`, or `"OTHER"`
- `hasBankDetails` (boolean, MoR only): Whether bank details are provided
- `message` (string): Human-readable status message

**Error Responses**:
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to get payment account status: <error_message>"}`

**Implementation Notes**:
- **Call this endpoint first** when vendor opens payment settings
- Check `payoutModel` field to determine which UI to show:
  - `"CONNECT"` → Show Stripe Connect UI (use existing Stripe endpoints)
  - `"MOR"` → Show MoR UI (use MoR-specific endpoints)
- Store `payoutModel` in app state to avoid repeated API calls
- Use `isReady` to show appropriate status indicators

---

## Unified Endpoints (Work with Both Models)

These endpoints work regardless of payment model and automatically route to the correct backend logic.

### Get Payment Account Status

**Endpoint**: `GET /vendors/payment/status`

See Step 1 above for details. This is the primary endpoint to determine payment model.

### Get Payment Onboarding Link

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

**Implementation Notes**:
- For CONNECT: Open URL in WebView for Stripe onboarding
- For MoR: Navigate to bank details form in your app
- This endpoint automatically creates accounts if they don't exist

---

## Stripe Connect Endpoints (CONNECT Model Only)

These endpoints are only available/functional for vendors using the CONNECT model. If a MoR vendor tries to access these, they will receive an error.

### Get Stripe Account Status

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

**Implementation Notes**:
- Only call this if `payoutModel === "CONNECT"`
- Use to check if Stripe onboarding is complete

### Get Stripe Onboarding Link

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

**Implementation Notes**:
- Only for CONNECT vendors
- Open in WebView for Stripe-hosted onboarding
- Handle return URL redirect after completion

### Get Stripe Account Details

**Endpoint**: `GET /vendors/stripe/account`

**Response** (200 OK):
```json
{
  "accountId": "acct_xxxxx",
  "email": "vendor@example.com",
  "country": "DE",
  "defaultCurrency": "eur",
  "type": "express",
  "chargesEnabled": true,
  "payoutsEnabled": true,
  "detailsSubmitted": true,
  "businessType": "company",
  "businessProfileName": "My Business"
}
```

### Get Stripe Balance

**Endpoint**: `GET /vendors/stripe/balance`

**Response** (200 OK):
```json
{
  "available": [
    {
      "amount": 75000,
      "currency": "eur",
      "sources": []
    }
  ],
  "pending": [
    {
      "amount": 25000,
      "currency": "eur",
      "sources": []
    }
  ],
  "instantAvailable": []
}
```

### Get Stripe Payouts

**Endpoint**: `GET /vendors/stripe/payouts?limit=50`

**Response** (200 OK):
```json
[
  {
    "id": "po_xxxxx",
    "amount": 50000,
    "currency": "eur",
    "status": "paid",
    "arrivalDate": 1699123456,
    "created": 1699037056,
    "description": "STRIPE PAYOUT"
  }
]
```

### Get Stripe Transactions

**Endpoint**: `GET /vendors/stripe/transactions?limit=50`

**Response** (200 OK):
```json
[
  {
    "id": "txn_xxxxx",
    "amount": 10000,
    "currency": "eur",
    "type": "charge",
    "status": "succeeded",
    "created": 1699037056,
    "description": "Payment for order #123"
  }
]
```

**Note**: See `MOBILE_APP_STRIPE_ACCOUNT_MANAGEMENT_PROMPT.md` for complete Stripe endpoint documentation.

---

## MoR (Merchant of Record) Endpoints (MoR Model Only)

These endpoints are only available/functional for vendors using the MoR model. If a CONNECT vendor tries to access these, they will receive an error.

### 1. Get MoR Balance

**Endpoint**: `GET /vendors/mor/balance`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "balance": 125000,
  "currency": "EUR",
  "payoutModel": "MOR",
  "hasBankDetails": true
}
```

**Response Fields**:
- `balance` (number): Current balance in cents (divide by 100 for display)
- `currency` (string): Currency code (e.g., "EUR", "USD")
- `payoutModel` (string): Always "MOR" for this endpoint
- `hasBankDetails` (boolean): Whether bank details are configured

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor is not using MoR model"}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve balance: <error_message>"}`

**Implementation Notes**:
- Display balance prominently (e.g., large number at top of MoR dashboard)
- Format: `balance / 100` + currency symbol (e.g., "€1,250.00")
- Show warning if `hasBankDetails` is false (vendor needs to add bank details)

### 2. Get MoR Transaction History

**Endpoint**: `GET /vendors/mor/transactions?limit=50`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters**:
- `limit` (optional): Maximum number of transactions (default: 50, max: 100)

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "amount": 10000,
    "currency": "EUR",
    "type": "ORDER_PAYMENT",
    "description": "Payment for order #123",
    "orderId": 123,
    "payoutId": null,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": 2,
    "amount": -5000,
    "currency": "EUR",
    "type": "PAYOUT",
    "description": "Manual payout request",
    "orderId": null,
    "payoutId": 1,
    "createdAt": "2024-01-20T14:20:00Z"
  }
]
```

**Response Fields**:
- `id` (number): Transaction ID
- `amount` (number): Amount in cents (positive = credit, negative = debit)
- `currency` (string): Currency code
- `type` (string): `"ORDER_PAYMENT"`, `"PAYOUT"`, `"ADJUSTMENT"`, or `"REFUND"`
- `description` (string): Transaction description
- `orderId` (number, nullable): Related order ID if applicable
- `payoutId` (number, nullable): Related payout ID if applicable
- `createdAt` (string): ISO 8601 timestamp

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor is not using MoR model"}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve transactions: <error_message>"}`

**Implementation Notes**:
- Display transactions in chronological order (newest first)
- Show positive amounts in green (credits), negative in red (debits)
- Format amounts: `Math.abs(amount) / 100` + currency
- Group by date for better UX
- Show transaction type with appropriate icon
- Link to order details if `orderId` is present

### 3. Submit Bank Details

**Endpoint**: `POST /vendors/mor/bank-details`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "holderName": "John Doe",
  "accountNumber": "1234567890",
  "bankName": "National Bank of Serbia",
  "swiftCode": "NBSRRSBG",
  "iban": "RS35123456789012345678",
  "payoutMethod": "BANK"
}
```

**Request Fields**:
- `holderName` (string, required): Bank account holder name
- `accountNumber` (string, required): Bank account number
- `bankName` (string, required): Bank name
- `swiftCode` (string, optional): SWIFT/BIC code
- `iban` (string, optional): IBAN (if applicable)
- `payoutMethod` (string, required): `"BANK"`, `"WISE"`, or `"OTHER"`

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Bank details submitted successfully"
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor is not using MoR model"}` or validation errors
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to submit bank details: <error_message>"}`

**Implementation Notes**:
- Create a form with all required fields
- Validate IBAN format if provided
- Validate SWIFT code format if provided
- Show success message and refresh payment status after submission
- Store bank details securely (consider encryption in production)
- Allow vendor to update bank details later

### 4. Get MoR Payout History

**Endpoint**: `GET /vendors/mor/payouts`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "amount": 50000,
    "currency": "EUR",
    "method": "BANK",
    "status": "PENDING",
    "requestedAt": "2024-01-20T10:00:00Z",
    "processedAt": null,
    "transactionReference": null
  },
  {
    "id": 2,
    "amount": 30000,
    "currency": "EUR",
    "method": "BANK",
    "status": "COMPLETED",
    "requestedAt": "2024-01-15T09:00:00Z",
    "processedAt": "2024-01-16T14:30:00Z",
    "transactionReference": "TXN-2024-001234"
  }
]
```

**Response Fields**:
- `id` (number): Payout ID
- `amount` (number): Amount in cents
- `currency` (string): Currency code
- `method` (string): `"BANK"`, `"WISE"`, or `"OTHER"`
- `status` (string): `"PENDING"`, `"PROCESSING"`, `"COMPLETED"`, or `"FAILED"`
- `requestedAt` (string): ISO 8601 timestamp when payout was requested
- `processedAt` (string, nullable): ISO 8601 timestamp when payout was processed
- `transactionReference` (string, nullable): Bank transfer reference number

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor is not using MoR model"}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve payouts: <error_message>"}`

**Implementation Notes**:
- Display payouts in reverse chronological order (newest first)
- Show status with appropriate color/icon:
  - PENDING: Yellow/orange
  - PROCESSING: Blue
  - COMPLETED: Green
  - FAILED: Red
- Format amounts: `amount / 100` + currency
- Show processing time estimate for PENDING payouts
- Display transaction reference for COMPLETED payouts

### 5. Request Manual Payout

**Endpoint**: `POST /vendors/mor/request-payout`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "amount": 50000,
  "currency": "EUR",
  "description": "Monthly payout request"
}
```

**Request Fields**:
- `amount` (number, required): Amount in cents (must be ≤ current balance)
- `currency` (string, optional): Currency code (defaults to "EUR")
- `description` (string, optional): Payout description

**Response** (200 OK):
```json
{
  "success": true,
  "payoutId": "1",
  "message": "Payout request created successfully. It will be processed manually."
}
```

**Error Responses**:
- **400 Bad Request**: 
  - `{"error": "Vendor is not using MoR model"}`
  - `{"error": "Payout method not configured. Please submit bank details first."}`
  - `{"error": "Insufficient balance. Available: 30000, Requested: 50000"}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to request payout: <error_message>"}`

**Implementation Notes**:
- Validate amount is positive and ≤ current balance
- Show current balance before allowing payout request
- Display confirmation dialog before submitting
- Show success message with payout ID
- Refresh balance and payout list after successful request
- Warn vendor that payout will be processed manually (may take 3-5 business days)

---

## UI/UX Guidelines

### Payment Settings Screen

Create a unified payment settings screen that adapts based on `payoutModel`:

**For CONNECT vendors:**
- Show Stripe account status
- Display Stripe balance
- Show Stripe payouts and transactions
- Link to Stripe account management

**For MoR vendors:**
- Show current balance prominently
- Display transaction history
- Show bank details (if configured) or link to add bank details
- Show payout history
- Button to request payout

### Determining Payment Model

```javascript
// Pseudo-code example
async function loadPaymentSettings() {
  const status = await fetch('/vendors/payment/status', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  const data = await status.json();
  
  if (data.payoutModel === 'CONNECT') {
    // Show Stripe Connect UI
    showStripeDashboard(data);
  } else if (data.payoutModel === 'MOR') {
    // Show MoR UI
    showMoRDashboard(data);
  }
}
```

### State Management

- Store `payoutModel` in app state after first API call
- Use this to conditionally render appropriate UI components
- Refresh payment status when vendor returns to payment screen

### Error Handling

- If vendor tries to access MoR endpoint but has CONNECT model (or vice versa), show friendly error:
  - "This feature is only available for [MoR/Stripe Connect] vendors."
- Handle network errors gracefully
- Show loading states during API calls

---

## Complete Workflow Examples

### Workflow 1: CONNECT Vendor (e.g., Germany)

1. Vendor opens payment settings
2. App calls `GET /vendors/payment/status`
3. Response: `payoutModel: "CONNECT"`
4. App shows Stripe Connect dashboard:
   - Stripe account status
   - Stripe balance
   - Stripe payouts
   - Stripe transactions
5. If not onboarded: Show "Complete Stripe Onboarding" button
6. If onboarded: Show account details and balance

### Workflow 2: MoR Vendor (e.g., Serbia)

1. Vendor opens payment settings
2. App calls `GET /vendors/payment/status`
3. Response: `payoutModel: "MOR"`, `balance: 125000`, `hasBankDetails: false`
4. App shows MoR dashboard:
   - Current balance: €1,250.00
   - Warning: "Please add bank details to receive payouts"
   - Transaction history (empty initially)
5. Vendor taps "Add Bank Details"
6. App shows bank details form
7. Vendor submits bank details via `POST /vendors/mor/bank-details`
8. App refreshes status, now shows `hasBankDetails: true`
9. After customer makes payment, balance updates automatically
10. Vendor can request payout via `POST /vendors/mor/request-payout`

### Workflow 3: MoR Vendor Requesting Payout

1. Vendor has balance: €500.00 (50000 cents)
2. Vendor taps "Request Payout"
3. App shows payout form with:
   - Current balance: €500.00
   - Amount input (max: €500.00)
   - Description field (optional)
4. Vendor enters amount: €300.00 (30000 cents)
5. App validates: amount ≤ balance ✓
6. App calls `POST /vendors/mor/request-payout` with:
   ```json
   {
     "amount": 30000,
     "currency": "EUR",
     "description": "Monthly payout"
   }
   ```
7. Response: `{"success": true, "payoutId": "1", ...}`
8. App shows success message: "Payout request #1 created. It will be processed within 3-5 business days."
9. App refreshes balance (now €200.00) and payout list

---

## Testing Checklist

### CONNECT Vendor Flow
- [ ] Payment status shows `payoutModel: "CONNECT"`
- [ ] Stripe account status displays correctly
- [ ] Stripe balance shows available and pending amounts
- [ ] Stripe payouts list displays correctly
- [ ] Stripe transactions list displays correctly
- [ ] Onboarding link opens Stripe onboarding page
- [ ] Return from Stripe onboarding updates status

### MoR Vendor Flow
- [ ] Payment status shows `payoutModel: "MOR"`
- [ ] Balance displays correctly (formatted with currency)
- [ ] Transaction history displays correctly
- [ ] Bank details form validates input
- [ ] Bank details submission succeeds
- [ ] Payout history displays correctly
- [ ] Payout request validates amount ≤ balance
- [ ] Payout request succeeds and updates balance
- [ ] Error handling for insufficient balance works

### Error Handling
- [ ] 401 Unauthorized redirects to login
- [ ] Network errors show user-friendly messages
- [ ] Wrong model endpoint access shows appropriate error
- [ ] Invalid input validation works

---

## Important Notes

1. **Always check `payoutModel` first** before showing any payment UI
2. **MoR balance is in cents** - divide by 100 for display
3. **Payouts are manual** - inform vendors of processing time (3-5 business days)
4. **Bank details are required** for MoR vendors to receive payouts
5. **Balance updates automatically** after customer payments (no action needed from vendor)
6. **Stripe endpoints only work for CONNECT vendors** - check model before calling
7. **MoR endpoints only work for MoR vendors** - check model before calling

---

## API Response Examples

### Payment Status - CONNECT Vendor
```json
{
  "isReady": true,
  "hasAccount": true,
  "provider": "STRIPE",
  "payoutModel": "CONNECT",
  "accountId": "acct_xxxxx",
  "country": "DE",
  "stripeSupported": true,
  "message": "Your payment account is ready to receive payments."
}
```

### Payment Status - MoR Vendor
```json
{
  "isReady": true,
  "hasAccount": true,
  "provider": "MOR",
  "payoutModel": "MOR",
  "accountId": "vendor@example.com",
  "country": "RS",
  "stripeSupported": false,
  "balance": 125000,
  "manualPayoutMethod": "BANK",
  "hasBankDetails": true,
  "message": "Your payment account is ready to receive payments."
}
```

---

## Summary

The mobile app must:
1. **Detect payment model** using `GET /vendors/payment/status`
2. **Show appropriate UI** based on `payoutModel` field
3. **Handle both models** seamlessly without vendor confusion
4. **Provide clear feedback** about payment status and requirements
5. **Guide vendors** through onboarding for their specific model

The backend automatically determines the payment model based on the vendor's country, so the mobile app just needs to adapt its UI accordingly.

---

## Security Considerations

1. **Token Storage**: Store JWT in secure storage (Keychain/SecureStorage), never in UserDefaults/SharedPreferences
2. **HTTPS Only**: Always use HTTPS in production
3. **Token Refresh**: Implement token refresh before expiration
4. **Bank Details**: Handle bank account information securely - consider encryption for sensitive fields
5. **Error Messages**: Don't expose sensitive error details to users
6. **Input Validation**: Validate all user inputs before sending to API
7. **Amount Validation**: Always validate payout amounts on client side before API call

## UI Components to Create

### 1. Payment Model Detection Component
- Calls `GET /vendors/payment/status` on mount
- Stores `payoutModel` in state
- Conditionally renders CONNECT or MoR UI

### 2. Stripe Connect Dashboard (for CONNECT vendors)
- Account status card
- Balance display (available + pending)
- Payouts list
- Transactions list
- Onboarding button (if not ready)

### 3. MoR Dashboard (for MoR vendors)
- Balance display (large, prominent)
- Transaction history list
- Bank details card (with edit button)
- Payout history list
- Request payout button

### 4. Bank Details Form
- Form fields: holderName, accountNumber, bankName, swiftCode, iban
- Payout method selector (BANK, WISE, OTHER)
- Validation and error handling
- Submit button

### 5. Payout Request Form
- Current balance display
- Amount input (with max validation)
- Description input (optional)
- Submit button with confirmation

## State Management Recommendations

```javascript
// Example state structure
{
  paymentModel: null, // 'CONNECT' or 'MOR'
  paymentStatus: {
    isReady: false,
    hasAccount: false,
    balance: 0,
    hasBankDetails: false,
    // ... other status fields
  },
  stripeData: {
    account: null,
    balance: null,
    payouts: [],
    transactions: []
  },
  morData: {
    balance: 0,
    transactions: [],
    payouts: [],
    bankDetails: null
  },
  loading: false,
  error: null
}
```

## Deep Linking (for Stripe Return)

If using Stripe Connect onboarding, handle deep links for return URL:

**Return URL Format**: `your-app://stripe-return`

**Implementation**:
1. Register deep link handler in app
2. When Stripe redirects, app receives deep link
3. Call `GET /vendors/stripe/return` to verify status
4. Update UI based on response

## Error Messages for Users

### MoR Vendor Tries Stripe Endpoint
**Message**: "This feature is only available for Stripe Connect vendors. Your account uses the Merchant of Record model. Please use the MoR payment features instead."

### CONNECT Vendor Tries MoR Endpoint
**Message**: "This feature is only available for Merchant of Record vendors. Your account uses Stripe Connect. Please use the Stripe payment features instead."

### Insufficient Balance for Payout
**Message**: "Insufficient balance. Your current balance is €X.XX. Please request a smaller amount or wait for more payments to be processed."

### Bank Details Not Configured
**Message**: "Bank details are required to receive payouts. Please add your bank account information to continue."

## Currency Formatting

Always format amounts correctly:
- **Display**: Divide by 100 and add currency symbol
- **Input**: Multiply by 100 before sending to API
- **Example**: 
  - Display: `125000 / 100 = 1250` → "€1,250.00"
  - Input: User enters "300.50" → Send `30050` to API

## Loading States

Show appropriate loading indicators for:
- Initial payment status check
- Fetching balance/transactions/payouts
- Submitting bank details
- Requesting payout
- Refreshing data

## Refresh Strategy

- **Pull to refresh**: Allow users to refresh payment data
- **Auto-refresh**: Refresh balance every 30-60 seconds when on payment screen
- **After actions**: Refresh data after submitting bank details or requesting payout

## Support Contacts

If vendor encounters issues:
- Show in-app support contact option
- Provide email: support@stillfresh.com (or your support email)
- Include error code and timestamp in support requests
- For MoR payouts: Explain that payouts are processed manually and may take 3-5 business days

---

## Quick Reference

### Key Endpoint: Payment Status
```
GET /vendors/payment/status
→ Returns payoutModel: "CONNECT" or "MOR"
→ Use this to determine which UI to show
```

### CONNECT Vendor Endpoints
- `GET /vendors/stripe/account-status`
- `GET /vendors/stripe/onboarding-link`
- `GET /vendors/stripe/account`
- `GET /vendors/stripe/balance`
- `GET /vendors/stripe/payouts`
- `GET /vendors/stripe/transactions`

### MoR Vendor Endpoints
- `GET /vendors/mor/balance`
- `GET /vendors/mor/transactions`
- `POST /vendors/mor/bank-details`
- `GET /vendors/mor/payouts`
- `POST /vendors/mor/request-payout`

### Unified Endpoints (Both Models)
- `GET /vendors/payment/status` ⭐ **Most Important**
- `POST /vendors/payment/onboarding-link`

---

**Last Updated**: Based on API version as of implementation date
**API Base URL**: Configure based on environment (dev/staging/production)
**Related Documents**: 
- `MOBILE_APP_STRIPE_ONBOARDING_PROMPT.md` - Stripe onboarding details
- `MOBILE_APP_STRIPE_ACCOUNT_MANAGEMENT_PROMPT.md` - Stripe account management

