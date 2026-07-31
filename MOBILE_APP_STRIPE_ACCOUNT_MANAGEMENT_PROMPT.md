# Mobile App AI Agent Prompt: Vendor Stripe Account Management Integration

## Overview
You are an AI agent responsible for implementing Stripe Connect account management features for vendors in a mobile application. The backend API provides endpoints that allow vendors to view and manage their Stripe account details, payouts, balance, bank accounts, transactions, and verification requirements after completing initial onboarding.

**Prerequisites**: This implementation assumes vendors have already completed Stripe onboarding using the endpoints documented in `MOBILE_APP_STRIPE_ONBOARDING_PROMPT.md`. Vendors must have an active Stripe account before using these management endpoints.

## Base API Configuration
- **Base URL**: `http://localhost:8083` (development) or your production API gateway URL
- **Authentication**: JWT Bearer token required for all endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

## Authentication Flow

### Vendor Login
Before accessing Stripe account management endpoints, the vendor must be authenticated.

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

## Account Management Endpoints

### 1. Get Account Details

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
  "country": "DE",
  "defaultCurrency": "eur",
  "type": "express",
  "chargesEnabled": true,
  "payoutsEnabled": true,
  "detailsSubmitted": true,
  "businessType": "company",
  "businessProfileName": "My Business",
  "capabilities": {
    "hasCapabilities": true
  },
  "requirements": {
    "currentlyDue": [],
    "pastDue": [],
    "pendingVerification": [],
    "disabledReason": null,
    "currentDeadline": null
  }
}
```

**Response Fields**:
- `accountId` (string): Stripe Connect account ID
- `email` (string): Account email address
- `country` (string): Country code (e.g., "DE", "US")
- `defaultCurrency` (string): Default currency code (e.g., "eur", "usd")
- `type` (string): Account type ("express", "standard", "custom")
- `chargesEnabled` (boolean): Whether account can receive charges
- `payoutsEnabled` (boolean): Whether account can receive payouts
- `detailsSubmitted` (boolean): Whether required details have been submitted
- `businessType` (string): "individual" or "company"
- `businessProfileName` (string): Business name
- `capabilities` (object): Account capabilities information
- `requirements` (object): Verification requirements (see Requirements endpoint for details)

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve account details: <error_message>"}`

**Implementation Notes**:
- Call this endpoint when vendor opens the "Account Details" or "Payment Settings" screen
- Display account information in a readable format
- Show verification status clearly (chargesEnabled, payoutsEnabled)
- If `requirements.currentlyDue` or `requirements.pastDue` has items, show a warning and link to complete requirements
- Cache this data and refresh periodically or on pull-to-refresh

---

### 2. Get Stripe Dashboard Login Link

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
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to create login link: <error_message>"}`

**Implementation Notes**:
- This generates a one-time login link to Stripe's Express Dashboard
- Open the `loginUrl` in an in-app browser (WebView) or external browser
- The link expires after a short period (typically 5-10 minutes)
- Use this when vendor wants to access Stripe's full dashboard for advanced settings
- Show loading indicator while generating the link
- Handle case where vendor closes browser - they can request a new link

---

### 3. Get Payout History

**Endpoint**: `GET /vendors/stripe/payouts?limit=10`

**Query Parameters**:
- `limit` (optional): Maximum number of payouts to retrieve (default: 10, max: 100)

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
[
  {
    "payoutId": "po_xxxxx",
    "amount": 50000,
    "currency": "eur",
    "status": "paid",
    "arrivalDate": "2024-01-15T10:00:00Z",
    "created": "2024-01-10T08:00:00Z",
    "description": null,
    "destination": "ba_xxxxx",
    "failureCode": null,
    "failureMessage": null,
    "method": "standard",
    "statementDescriptor": null
  }
]
```

**Response Fields**:
- `payoutId` (string): Stripe payout ID
- `amount` (number): Payout amount in cents (divide by 100 for display)
- `currency` (string): Currency code (e.g., "eur", "usd")
- `status` (string): `paid`, `pending`, `in_transit`, `canceled`, or `failed`
- `arrivalDate` (string): ISO 8601 date when payout arrives in bank account
- `created` (string): ISO 8601 date when payout was created
- `method` (string): `standard` or `instant`
- `failureCode` (string|null): Error code if payout failed
- `failureMessage` (string|null): Human-readable error message if payout failed
- `destination` (string): Bank account ID where payout is sent

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve payouts: <error_message>"}`

**Implementation Notes**:
- Display payouts in reverse chronological order (newest first)
- Format amounts: divide by 100 and show currency symbol (e.g., "€500.00")
- Color-code status: green for `paid`, yellow for `pending`/`in_transit`, red for `failed`/`canceled`
- Show arrival date prominently for pending payouts
- If `failureMessage` exists, display it prominently with a warning icon
- Implement pull-to-refresh functionality
- Allow vendor to tap on payout to see details (use endpoint #4)
- Show empty state if no payouts exist

---

### 4. Get Specific Payout

**Endpoint**: `GET /vendors/stripe/payouts/{payoutId}`

**Path Parameters**:
- `payoutId` (required): Stripe payout ID (e.g., "po_xxxxx")

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "payoutId": "po_xxxxx",
  "amount": 50000,
  "currency": "eur",
  "status": "paid",
  "arrivalDate": "2024-01-15T10:00:00Z",
  "created": "2024-01-10T08:00:00Z",
  "description": null,
  "destination": "ba_xxxxx",
  "failureCode": null,
  "failureMessage": null,
  "method": "standard",
  "statementDescriptor": null
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve payout: <error_message>"}`

**Implementation Notes**:
- Use this endpoint when vendor taps on a payout in the list to see details
- Show all payout information in a detail screen
- Link `destination` to bank account details if available
- Display status with appropriate visual indicator
- Show timeline: Created → Status → Arrival Date (if pending)

---

### 5. Get Account Balance

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

**Response Fields**:
- `available` (array): Balance available for immediate payout
- `pending` (array): Balance pending (not yet available for payout)
- `instantAvailable` (array): Balance available for instant payout (if enabled)
- Each balance object contains:
  - `amount` (number): Amount in cents (divide by 100 for display)
  - `currency` (string): Currency code
  - `sources` (array): Source types (can be empty)

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve balance: <error_message>"}`

**Implementation Notes**:
- Display available balance prominently (e.g., large number at top of screen)
- Show pending balance separately with explanation: "Pending: will be available for payout in X days"
- Format amounts: divide by 100 and show currency symbol
- Calculate and display total balance = available + pending
- Update balance periodically (e.g., every 30 seconds) when on balance screen
- Implement pull-to-refresh
- Show currency symbol based on account's default currency

---

### 6. Get Verification Requirements

**Endpoint**: `GET /vendors/stripe/requirements`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "currentlyDue": ["external_account", "representative.phone"],
  "eventuallyDue": ["business_profile.url"],
  "pastDue": [],
  "pendingVerification": ["representative.verification.document"],
  "disabledReason": null,
  "currentDeadline": 1735689600,
  "eventuallyDeadline": null
}
```

**Response Fields**:
- `currentlyDue` (array): Fields that must be provided immediately
- `eventuallyDue` (array): Fields that will be required in the future
- `pastDue` (array): Fields that are overdue
- `pendingVerification` (array): Fields waiting for Stripe verification
- `disabledReason` (string|null): Reason account is disabled (if applicable)
- `currentDeadline` (number|null): Unix timestamp for current deadline
- `eventuallyDeadline` (number|null): Unix timestamp for eventual deadline (may be null)

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve requirements: <error_message>"}`

**Implementation Notes**:
- Show requirements prominently if any are due
- Display `pastDue` items with highest priority (red warning badge)
- Show `currentlyDue` items with medium priority (yellow warning badge)
- `pendingVerification` items should show "Verification in progress" status (blue info badge)
- If `disabledReason` exists, show it prominently and explain impact
- Convert Unix timestamps to readable dates for deadlines
- Provide action button to complete requirements (opens Stripe onboarding/update link)
- Show empty state if all requirements are met
- Group requirements by category for better UX

---

### 7. Get Bank Accounts

**Endpoint**: `GET /vendors/stripe/bank-accounts`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
[
  {
    "bankAccountId": "ba_xxxxx",
    "accountHolderName": "John Doe",
    "accountHolderType": "individual",
    "bankName": "Deutsche Bank",
    "country": "DE",
    "currency": "eur",
    "last4": "1234",
    "routingNumber": "12345678",
    "status": "verified",
    "defaultForCurrency": true,
    "fingerprint": "xxxxx"
  }
]
```

**Response Fields**:
- `bankAccountId` (string): Stripe bank account ID
- `accountHolderName` (string): Name on the bank account
- `accountHolderType` (string): `individual` or `company`
- `bankName` (string): Name of the bank
- `country` (string): Country code
- `currency` (string): Currency code
- `last4` (string): Last 4 digits of account number
- `routingNumber` (string): Bank routing number
- `status` (string): `new`, `validated`, `verified`, `verification_failed`, or `errored`
- `defaultForCurrency` (boolean): Whether this is the default account for its currency
- `fingerprint` (string): Unique identifier for the account

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve bank accounts: <error_message>"}`

**Implementation Notes**:
- Display bank accounts in a list
- Show masked account number: "****1234" or "••••1234"
- Indicate default account with a badge or checkmark
- Show status with appropriate color (green for verified, yellow for validated, red for failed)
- Allow vendor to set default account (see endpoint #10)
- Allow vendor to delete non-default accounts (see endpoint #9)
- Show empty state with "Add Bank Account" button if no accounts exist
- Implement pull-to-refresh

---

### 8. Add Bank Account

**Endpoint**: `POST /vendors/stripe/bank-accounts`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/x-www-form-urlencoded
```

**Request Parameters**:
- `bankAccountToken` (required): Token from Stripe.js or Stripe Elements

**Response** (200 OK):
```json
{
  "bankAccountId": "ba_xxxxx",
  "accountHolderName": "John Doe",
  "accountHolderType": "individual",
  "bankName": "Deutsche Bank",
  "country": "DE",
  "currency": "eur",
  "last4": "1234",
  "routingNumber": "12345678",
  "status": "new",
  "defaultForCurrency": false,
  "fingerprint": "xxxxx"
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **400 Bad Request**: `{"error": "Invalid bank account token."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to create bank account: <error_message>"}`

**Implementation Notes**:
- **CRITICAL**: You must use Stripe.js or Stripe Elements to collect bank account details securely
- **NEVER** send raw bank account numbers, routing numbers, or account holder information to your backend
- Use Stripe's `createToken` or `createPaymentMethod` to get a token
- Send only the token to your backend
- After successful creation, refresh the bank accounts list
- Show verification status (may be "new" initially, then "validated" after micro-deposits)
- Display success message: "Bank account added successfully. Verification may take 1-2 business days."

**Stripe Integration Requirements**:
- For React Native: Use `@stripe/stripe-react-native` package
- For Flutter: Use `stripe_payment` or `flutter_stripe` package
- For native iOS: Use Stripe iOS SDK
- For native Android: Use Stripe Android SDK

**Example Flow**:
1. Vendor taps "Add Bank Account"
2. Show form with Stripe Elements/Bank Account component
3. Vendor enters account details (handled securely by Stripe)
4. On submit, create token using Stripe SDK
5. Send token to backend endpoint
6. Show success message
7. Refresh bank accounts list

---

### 9. Delete Bank Account

**Endpoint**: `DELETE /vendors/stripe/bank-accounts/{bankAccountId}`

**Path Parameters**:
- `bankAccountId` (required): Stripe bank account ID (e.g., "ba_xxxxx")

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Bank account deleted successfully"
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **400 Bad Request**: `{"error": "Cannot delete default bank account."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to delete bank account: <error_message>"}`

**Implementation Notes**:
- Show confirmation dialog before deleting: "Are you sure you want to delete this bank account?"
- Prevent deletion of default bank account (disable delete button or show error if attempted)
- After successful deletion, refresh the bank accounts list
- If this was the only bank account, show a message encouraging vendor to add a new one
- Show success message: "Bank account deleted successfully"

---

### 10. Set Default Bank Account

**Endpoint**: `POST /vendors/stripe/bank-accounts/{bankAccountId}/default`

**Path Parameters**:
- `bankAccountId` (required): Stripe bank account ID (e.g., "ba_xxxxx")

**Query Parameters**:
- `currency` (required): Currency code (e.g., "eur", "usd")

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "bankAccountId": "ba_xxxxx",
  "accountHolderName": "John Doe",
  "accountHolderType": "individual",
  "bankName": "Deutsche Bank",
  "country": "DE",
  "currency": "eur",
  "last4": "1234",
  "routingNumber": "12345678",
  "status": "verified",
  "defaultForCurrency": true,
  "fingerprint": "xxxxx"
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to set default bank account: <error_message>"}`

**Implementation Notes**:
- Use the account's currency from the bank account object
- After setting default, refresh the bank accounts list to show updated default status
- Show success message: "Default bank account updated"
- Update UI immediately to reflect new default status

---

### 11. Get Transaction History

**Endpoint**: `GET /vendors/stripe/transactions?limit=50`

**Query Parameters**:
- `limit` (optional): Maximum number of transactions to retrieve (default: 10, max: 100, recommended: 50 for initial load)

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
[
  {
    "transactionId": "txn_xxxxx",
    "amount": 50000,
    "currency": "eur",
    "description": "Payment for order #12345",
    "created": "2024-01-10T08:00:00Z",
    "type": "charge",
    "status": "available",
    "fee": 1750,
    "net": 48250,
    "source": "ch_xxxxx",
    "reportingCategory": "charge"
  }
]
```

**Response Fields**:
- `transactionId` (string): Stripe transaction ID
- `amount` (number): Transaction amount in cents (divide by 100 for display)
- `currency` (string): Currency code
- `description` (string|null): Transaction description
- `created` (string): ISO 8601 date when transaction was created
- `type` (string): Transaction type (e.g., `charge`, `refund`, `payout`, `transfer`, `adjustment`)
- `status` (string): `available` or `pending`
- `fee` (number): Stripe fee in cents
- `net` (number): Net amount after fees in cents
- `source` (string): Source transaction ID (e.g., charge ID, payout ID)
- `reportingCategory` (string): Category for reporting (e.g., `charge`, `refund`, `payout`)

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor does not have a Stripe account. Please complete onboarding first."}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve transactions: <error_message>"}`

**Implementation Notes**:
- Display transactions in reverse chronological order (newest first)
- Format amounts: divide by 100 and show currency symbol
- Show fee breakdown: "Amount: €500.00, Fee: -€17.50, Net: €482.50"
- Color-code by type: green for charges, red for refunds, blue for payouts
- Group transactions by date (Today, Yesterday, This Week, This Month, Older)
- Allow filtering by type (charges, refunds, payouts, etc.)
- Show transaction details when tapped
- Implement pull-to-refresh
- Show empty state if no transactions exist
- Display status indicator (available vs pending)

---

## Mobile App Implementation Guide

### 1. Payment Settings Dashboard Screen

**UI Flow**:
```
┌─────────────────────────────┐
│  Payment Settings           │
├─────────────────────────────┤
│  Account Status: ✅ Ready   │
│                             │
│  Balance: €750.00           │
│  Available: €500.00         │
│  Pending: €250.00          │
│                             │
│  [View Payouts]             │
│  [View Transactions]        │
│  [Manage Bank Accounts]     │
│  [Account Details]          │
│  [Stripe Dashboard]          │
└─────────────────────────────┘
```

**Implementation Steps**:
1. On screen load, call `GET /vendors/stripe/account-status` (from onboarding prompt)
2. Call `GET /vendors/stripe/balance` to show balance summary
3. Display account status badge (Ready/Pending/Not Set Up)
4. Show available and pending balance
5. Provide navigation buttons to:
   - Payouts List
   - Transactions List
   - Bank Accounts
   - Account Details
   - Stripe Dashboard (generates login link)

### 2. Account Details Screen

**When user navigates to Account Details**:

1. **Show Loading Indicator**
   - Display: "Loading account information..."

2. **Call API**: `GET /vendors/stripe/account`
   - Handle errors gracefully
   - Display account information in sections:
     - Account Information (ID, email, country, currency)
     - Business Information (name, type)
     - Verification Status (charges enabled, payouts enabled)
     - Requirements (link to requirements screen if items are due)

3. **Display Requirements Warning** (if applicable)
   - If `requirements.currentlyDue` or `requirements.pastDue` has items
   - Show warning banner with link to requirements screen

### 3. Balance Screen

**UI Flow**:
```
┌─────────────────────────────┐
│  Account Balance            │
├─────────────────────────────┤
│                             │
│      €750.00                │
│   Available Balance         │
│                             │
│  ─────────────────────      │
│                             │
│  Pending: €250.00           │
│  (Available in 2 days)      │
│                             │
│  [Refresh]                   │
└─────────────────────────────┘
```

**Implementation Steps**:
1. Call `GET /vendors/stripe/balance` on screen load
2. Display available balance prominently (large number)
3. Show pending balance with explanation
4. Calculate and show total balance
5. Implement pull-to-refresh
6. Auto-refresh every 30 seconds when screen is active

### 4. Payouts List Screen

**UI Flow**:
```
┌─────────────────────────────┐
│  Payouts                    │
├─────────────────────────────┤
│  [Filter] [Sort]             │
│                             │
│  ✅ €500.00                 │
│  Paid • Jan 15, 2024        │
│  ─────────────────────      │
│                             │
│  ⏳ €300.00                 │
│  Pending • Arrives Jan 20   │
│  ─────────────────────      │
└─────────────────────────────┘
```

**Implementation Steps**:
1. Call `GET /vendors/stripe/payouts?limit=20` on screen load
2. Display payouts in reverse chronological order
3. Show status badge with color coding
4. Format amounts with currency symbol
5. Show arrival date for pending payouts
6. Implement pull-to-refresh
7. Allow vendor to tap payout to see details (navigate to Payout Detail screen)
8. Show empty state if no payouts

### 5. Payout Detail Screen

**When user taps on a payout**:

1. **Show Loading Indicator**
2. **Call API**: `GET /vendors/stripe/payouts/{payoutId}`
3. **Display payout information**:
   - Amount (large display)
   - Status with timeline
   - Created date
   - Arrival date (if pending)
   - Method (standard/instant)
   - Failure message (if failed)
   - Bank account destination

### 6. Bank Accounts Screen

**UI Flow**:
```
┌─────────────────────────────┐
│  Bank Accounts               │
├─────────────────────────────┤
│  [Add Bank Account]          │
│                             │
│  ✓ Deutsche Bank            │
│  ••••1234 • Default          │
│  Verified                    │
│  [Set Default] [Delete]      │
│  ─────────────────────      │
│                             │
│  Commerzbank                 │
│  ••••5678                    │
│  Validated                   │
│  [Set Default] [Delete]      │
└─────────────────────────────┘
```

**Implementation Steps**:
1. Call `GET /vendors/stripe/bank-accounts` on screen load
2. Display bank accounts in a list
3. Show default badge on default account
4. Show status with color coding
5. Mask account numbers (show only last 4 digits)
6. Provide actions:
   - Set Default (for non-default accounts)
   - Delete (for non-default accounts)
7. Show "Add Bank Account" button
8. Implement pull-to-refresh

### 7. Add Bank Account Screen

**When user taps "Add Bank Account"**:

1. **Integrate Stripe Elements/SDK**
   - Use Stripe's secure bank account collection component
   - Never collect raw bank account details in your app

2. **Show Form**
   - Account holder name
   - Account holder type (individual/company)
   - Bank account details (handled by Stripe)

3. **On Submit**:
   - Create token using Stripe SDK
   - Call `POST /vendors/stripe/bank-accounts` with token
   - Show success message
   - Navigate back to Bank Accounts list
   - Refresh the list

4. **Handle Errors**:
   - Invalid token: "Invalid bank account information. Please try again."
   - Network error: "Unable to add bank account. Please check your connection."

### 8. Transactions List Screen

**UI Flow**:
```
┌─────────────────────────────┐
│  Transactions                │
├─────────────────────────────┤
│  [Filter] [Search]           │
│                             │
│  Today                       │
│  ─────────────────────      │
│  +€500.00 Charge             │
│  Order #12345                │
│  Fee: -€17.50               │
│  Net: €482.50               │
│  ─────────────────────      │
│                             │
│  Yesterday                   │
│  ─────────────────────      │
│  -€50.00 Refund             │
│  Order #12344               │
└─────────────────────────────┘
```

**Implementation Steps**:
1. Call `GET /vendors/stripe/transactions?limit=50` on screen load
2. Group transactions by date (Today, Yesterday, This Week, etc.)
3. Display transactions in reverse chronological order
4. Show transaction type with icon/color:
   - Green + for charges
   - Red - for refunds
   - Blue for payouts
5. Show fee breakdown (amount, fee, net)
6. Implement pull-to-refresh
7. Allow filtering by type
8. Allow vendor to tap transaction to see details
9. Show empty state if no transactions

### 9. Requirements Screen

**UI Flow**:
```
┌─────────────────────────────┐
│  Verification Requirements   │
├─────────────────────────────┤
│  ⚠️ Action Required          │
│                             │
│  Past Due (High Priority)   │
│  • External Account          │
│  • Representative Phone     │
│                             │
│  Currently Due              │
│  • Business Profile URL     │
│                             │
│  Pending Verification       │
│  • Identity Document        │
│                             │
│  [Complete Requirements]     │
└─────────────────────────────┘
```

**Implementation Steps**:
1. Call `GET /vendors/stripe/requirements` on screen load
2. Group requirements by priority:
   - Past Due (red, highest priority)
   - Currently Due (yellow, high priority)
   - Eventually Due (blue, medium priority)
   - Pending Verification (blue, info)
3. Show deadline dates if available
4. Show disabled reason if account is disabled
5. Provide "Complete Requirements" button that opens Stripe update link
6. Show success state if all requirements are met

### 10. Stripe Dashboard Access

**When user taps "Stripe Dashboard"**:

1. **Show Loading Indicator**
   - Display: "Generating dashboard link..."

2. **Call API**: `POST /vendors/stripe/login-link`
   - Handle errors gracefully

3. **Open Dashboard**
   - Extract `loginUrl` from response
   - Open in WebView or external browser
   - Handle case where vendor closes browser

---

## State Management

**Recommended State Variables**:
```typescript
interface StripeAccountState {
  accountDetails: AccountDetails | null;
  balance: Balance | null;
  payouts: Payout[];
  transactions: Transaction[];
  bankAccounts: BankAccount[];
  requirements: Requirements | null;
  isLoading: boolean;
  error: string | null;
  lastUpdated: Date | null;
}

interface AccountDetails {
  accountId: string;
  email: string;
  country: string;
  defaultCurrency: string;
  type: string;
  chargesEnabled: boolean;
  payoutsEnabled: boolean;
  detailsSubmitted: boolean;
  businessType: string;
  businessProfileName: string;
  capabilities: object;
  requirements: object;
}

interface Balance {
  available: BalanceAmount[];
  pending: BalanceAmount[];
  instantAvailable: BalanceAmount[];
}

interface Payout {
  payoutId: string;
  amount: number;
  currency: string;
  status: string;
  arrivalDate: string;
  created: string;
  method: string;
  failureMessage: string | null;
}

interface Transaction {
  transactionId: string;
  amount: number;
  currency: string;
  description: string | null;
  created: string;
  type: string;
  status: string;
  fee: number;
  net: number;
}

interface BankAccount {
  bankAccountId: string;
  accountHolderName: string;
  bankName: string;
  last4: string;
  status: string;
  defaultForCurrency: boolean;
}

interface Requirements {
  currentlyDue: string[];
  eventuallyDue: string[];
  pastDue: string[];
  pendingVerification: string[];
  disabledReason: string | null;
  currentDeadline: number | null;
}
```

**State Transitions**:
- `initial` → `loading` → `loaded` / `error`
- Handle refresh states: `refreshing` → `loaded`
- Handle individual operations: `operating` → `success` / `error`

---

## Error Handling

**Network Errors**:
- Show user-friendly message: "Unable to connect. Please check your internet connection."
- Provide retry button
- Show cached data if available

**API Errors**:
- **401 Unauthorized**: Token expired - redirect to login screen
- **400 Bad Request**: Show error message from response
- **500 Internal Server Error**: Show generic error with retry option
- **404 Not Found**: Show "Resource not found" message

**Stripe Errors**:
- Handle bank account validation errors gracefully
- Show user-friendly error messages
- Allow user to retry failed operations

---

## Data Formatting Guidelines

### Amounts
- Always divide by 100 to convert cents to currency units
- Format with 2 decimal places: `(amount / 100).toFixed(2)`
- Show currency symbol based on account currency:
  - EUR: `€${formattedAmount}`
  - USD: `$${formattedAmount}`
  - GBP: `£${formattedAmount}`

### Dates
- Parse ISO 8601 strings: `new Date(isoString)`
- Format for display:
  - Short: "Jan 15, 2024"
  - Long: "January 15, 2024"
  - Relative: "2 hours ago", "Yesterday", "3 days ago"
- Group by: Today, Yesterday, This Week, This Month, Older

### Status Colors
- **Success/Active/Verified**: Green (#4CAF50)
- **Pending/Warning/Validated**: Yellow/Orange (#FF9800)
- **Error/Failed/Canceled**: Red (#F44336)
- **Info/Neutral/New**: Blue (#2196F3)

### Status Icons
- ✅ Paid/Verified/Success
- ⏳ Pending/In Transit
- ⚠️ Warning/Validated
- ❌ Failed/Canceled
- ℹ️ Info/New

---

## Complete Flow Diagram

```
┌─────────────────────────────┐
│ Payment Settings Dashboard   │
└──────────────┬───────────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌──────────┐
│Balance │ │Payouts │ │Transactions│
└────────┘ └────┬───┘ └─────┬─────┘
                │           │
                ▼           ▼
        ┌──────────────┐ ┌──────────────┐
        │ Payout Detail│ │Transaction   │
        │              │ │Detail        │
        └──────────────┘ └──────────────┘
               │
               ▼
        ┌──────────────┐
        │ Bank Accounts│
        └──────┬───────┘
               │
        ┌──────┴───────┐
        │               │
        ▼               ▼
┌──────────────┐ ┌──────────────┐
│ Add Bank     │ │ Requirements │
│ Account      │ │              │
└──────────────┘ └──────────────┘
```

---

## Testing Checklist

- [ ] All endpoints return expected data when vendor has active account
- [ ] Error handling works for expired tokens (401)
- [ ] Error handling works for missing account (400)
- [ ] Error handling works for server errors (500)
- [ ] Balance updates correctly and shows available/pending
- [ ] Payouts list shows in reverse chronological order
- [ ] Payout detail screen displays all information correctly
- [ ] Bank accounts can be added (with Stripe token)
- [ ] Bank accounts can be deleted (non-default only)
- [ ] Default bank account can be set
- [ ] Transactions show correct fee calculations
- [ ] Transactions are grouped by date correctly
- [ ] Requirements display correctly when items are due
- [ ] Requirements show empty state when all met
- [ ] Pull-to-refresh works on all list screens
- [ ] Loading states display correctly
- [ ] Offline mode shows cached data (if implemented)
- [ ] Currency formatting is correct for all amounts
- [ ] Date formatting is consistent across app
- [ ] Status colors and icons are correct
- [ ] Empty states display when no data exists
- [ ] Confirmation dialogs work for destructive actions
- [ ] Stripe dashboard login link opens correctly

---

## Security Considerations

1. **Token Storage**: Store JWT in secure storage (Keychain/SecureStorage), never in UserDefaults/SharedPreferences
2. **HTTPS Only**: Always use HTTPS in production
3. **Token Refresh**: Implement automatic token refresh before expiration
4. **Bank Account Tokens**: Never log or store bank account tokens
5. **Sensitive Data**: Don't log full account numbers or routing numbers
6. **Error Messages**: Don't expose sensitive error details to users
7. **Stripe Integration**: Always use Stripe SDK for collecting bank account details - never collect raw data

---

## Additional Notes

- All amounts are in cents - always divide by 100 for display
- Dates are in ISO 8601 format - parse accordingly
- Some endpoints may return empty arrays if no data exists
- Bank account verification can take 1-2 business days (micro-deposits)
- Payouts typically take 2-7 business days depending on country
- Transaction fees vary by country and payment method
- Requirements may change over time as Stripe updates compliance needs
- Stripe dashboard login links expire after 5-10 minutes
- Default bank account can only be set for accounts in the same currency

---

## Support Contacts

If vendor encounters issues:
- Show in-app support contact option
- Provide email: support@stillfresh.com (or your support email)
- Include error code and timestamp in support requests

---

**Last Updated**: Based on API version as of implementation date
**API Base URL**: Configure based on environment (dev/staging/production)
**Related Documentation**: See `MOBILE_APP_STRIPE_ONBOARDING_PROMPT.md` for onboarding flow

