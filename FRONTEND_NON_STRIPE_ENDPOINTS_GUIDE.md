# Frontend MoR Payment Endpoints Integration Guide

This guide provides comprehensive documentation for integrating MoR (Merchant of Record) payment endpoints in the mobile app frontend. MoR is used for vendors in countries where Stripe Connect is not supported.

## Table of Contents
1. [Base Configuration](#base-configuration)
2. [Authentication](#authentication)
3. [Understanding MoR Model](#understanding-mor-model)
4. [Unified Payment Endpoints](#unified-payment-endpoints)
5. [MoR Payment Endpoints](#mor-payment-endpoints)
6. [Error Handling](#error-handling)
7. [Workflow Examples](#workflow-examples)
8. [Data Models](#data-models)

---

## Base Configuration

**Base URL**: `https://your-api-domain.com` (or `http://localhost:8083` for development)

**API Prefix**: `/vendors` (for vendor-service endpoints)

**Content-Type**: `application/json` for all requests

---

## Authentication

All MoR payment endpoints require authentication via JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

**Token Management**:
- Store token securely (e.g., SecureStore in React Native, Keychain in iOS)
- Token expires after a set period - implement refresh logic
- If token is invalid/expired (401 response), redirect user to login

---

## Understanding MoR Model

### What is MoR (Merchant of Record)?

MoR (Merchant of Record) is a payment model used for vendors in countries where Stripe Connect is not supported (e.g., Serbia, Bosnia, Montenegro, etc.).

### How MoR Works:

1. **Payment Collection**: Customer payments go to the platform's Stripe account (full amount)
2. **Platform Fee**: Platform fee is deducted from the payment
3. **Internal Balance**: Vendor's share is added to their internal `balance` (stored in database)
4. **Manual Payouts**: Vendors can request payouts, which are processed manually by platform admins via bank transfer, Wise, etc.

### Key Differences from Stripe Connect:

| Feature | Stripe Connect | MoR |
|---------|---------------|-----|
| Payment Destination | Directly to vendor's Stripe account | Platform's Stripe account |
| Payout Method | Automatic via Stripe | Manual by platform admins |
| Balance Tracking | Stripe balance | Internal database balance |
| Bank Account | Managed by Stripe | Submitted to platform |
| Payout Speed | Automatic (daily/weekly) | Manual processing |

### When to Use MoR Endpoints:

- Always check `payoutModel` from payment status endpoint first
- If `payoutModel === "MOR"`, use MoR endpoints
- If `payoutModel === "CONNECT"`, use Stripe endpoints (see Stripe guide)

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
  "payoutModel": "MOR",
  "paymentProvider": "MOR",
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
- If `payoutModel === "CONNECT"`, use Stripe endpoints (see Stripe guide)
- If `payoutModel === "MOR"`, use MoR endpoints (see below)

**Example Code**:
```javascript
const checkPaymentStatus = async () => {
  try {
    const token = await getStoredToken();
    const response = await fetch('/vendors/payment/status', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    if (response.ok) {
      const status = await response.json();
      
      // Store payout model in app state
      setPayoutModel(status.payoutModel);
      
      if (status.payoutModel === 'MOR') {
        // Use MoR endpoints
        return { success: true, isMoR: true, status };
      } else {
        // Use Stripe endpoints
        return { success: true, isMoR: false, status };
      }
    } else {
      const error = await response.json();
      return { success: false, error: error.error };
    }
  } catch (error) {
    return { success: false, error: error.message };
  }
};
```

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
  "onboardingUrl": "/vendors/mor/bank-details",
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

**Example Code**:
```javascript
const getOnboardingLink = async () => {
  try {
    const token = await getStoredToken();
    const response = await fetch('/vendors/payment/onboarding-link', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    if (response.ok) {
      const result = await response.json();
      
      if (payoutModel === 'MOR') {
        // Navigate to bank details form in app
        navigation.navigate('BankDetailsForm');
      } else {
        // Open Stripe onboarding in WebView
        openWebView(result.onboardingUrl);
      }
      
      return { success: true, onboardingUrl: result.onboardingUrl };
    } else {
      const error = await response.json();
      return { success: false, error: error.error };
    }
  } catch (error) {
    return { success: false, error: error.message };
  }
};
```

---

## MoR Payment Endpoints

These endpoints are **only available/functional for vendors using the MoR model** (`payoutModel === "MOR"`). If a CONNECT vendor tries to access these, they will receive an error.

### 3. Get MoR Balance

**Endpoint**: `GET /vendors/mor/balance`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "balance": 5000.00,
  "currency": "EUR",
  "formattedBalance": "€50.00"
}
```

**Response Fields**:
- `balance`: Balance amount in cents (number)
- `currency`: Currency code (string, e.g., "EUR", "USD")
- `formattedBalance`: Formatted balance string (string)

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor is not using MoR model"}` or `{"error": "Bank details not provided. Please submit bank details first."}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve balance: <message>"}`

**Implementation Notes**:
- Only call this if `payoutModel === "MOR"` (from payment status endpoint)
- Balance is in cents - divide by 100 for display: `(balance / 100).toFixed(2)`
- Display prominently in payment dashboard
- Refresh balance after payments or payouts
- If error about bank details, show bank details form

**Example Code**:
```javascript
const getMoRBalance = async () => {
  try {
    const token = await getStoredToken();
    const response = await fetch('/vendors/mor/balance', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    if (response.ok) {
      const balance = await response.json();
      return { success: true, data: balance };
    } else {
      const error = await response.json();
      
      // Check if bank details are missing
      if (error.error && error.error.includes('Bank details')) {
        // Show bank details form
        navigation.navigate('BankDetailsForm');
      }
      
      return { success: false, error: error.error };
    }
  } catch (error) {
    return { success: false, error: error.message };
  }
};
```

---

### 4. Get MoR Transactions

**Endpoint**: `GET /vendors/mor/transactions?limit=<number>`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters**:
- `limit` (optional): Maximum number of transactions to retrieve (default: 50)

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "vendorId": 1,
    "amount": 2500,
    "currency": "EUR",
    "type": "ORDER_PAYMENT",
    "description": "Payment for order #123",
    "orderId": 123,
    "payoutId": null,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": 2,
    "vendorId": 1,
    "amount": -5000,
    "currency": "EUR",
    "type": "PAYOUT",
    "description": "Manual payout request",
    "orderId": null,
    "payoutId": "payout_1",
    "createdAt": "2024-01-14T14:20:00Z"
  },
  {
    "id": 3,
    "vendorId": 1,
    "amount": -100,
    "currency": "EUR",
    "type": "ADJUSTMENT",
    "description": "Platform fee adjustment",
    "orderId": null,
    "payoutId": null,
    "createdAt": "2024-01-13T09:15:00Z"
  }
]
```

**Response Fields**:
- `id`: Transaction ID (number)
- `vendorId`: Vendor ID (number)
- `amount`: Amount in cents (positive for credits, negative for debits) (number)
- `currency`: Currency code (string)
- `type`: Transaction type - `"ORDER_PAYMENT"`, `"PAYOUT"`, `"ADJUSTMENT"`, `"REFUND"` (string)
- `description`: Transaction description (string)
- `orderId`: Related order ID if applicable (number or null)
- `payoutId`: Related payout ID if applicable (string or null)
- `createdAt`: ISO 8601 date-time (string)

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor is not using MoR model"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve transactions: <message>"}`

**Implementation Notes**:
- Display in chronological order (newest first)
- Show transaction type with icons/colors:
  - `ORDER_PAYMENT`: Green (credit)
  - `PAYOUT`: Red (debit)
  - `ADJUSTMENT`: Yellow/Orange
  - `REFUND`: Blue
- Format amounts with +/- indicators
- Show empty state if array is empty
- Implement pagination if needed (use `limit` parameter)

**Example Code**:
```javascript
const getMoRTransactions = async (limit = 50) => {
  try {
    const token = await getStoredToken();
    const response = await fetch(`/vendors/mor/transactions?limit=${limit}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    if (response.ok) {
      const transactions = await response.json();
      
      // Format transactions for display
      const formattedTransactions = transactions.map(txn => ({
        ...txn,
        formattedAmount: `${txn.amount >= 0 ? '+' : ''}${(txn.amount / 100).toFixed(2)} ${txn.currency.toUpperCase()}`,
        isCredit: txn.amount >= 0,
      }));
      
      return { success: true, data: formattedTransactions };
    } else {
      const error = await response.json();
      return { success: false, error: error.error };
    }
  } catch (error) {
    return { success: false, error: error.message };
  }
};
```

---

### 5. Submit Bank Details

**Endpoint**: `POST /vendors/mor/bank-details`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "bankAccountHolderName": "John Doe",
  "bankAccountNumber": "1234567890",
  "bankName": "Example Bank",
  "bankSwiftCode": "EXAMUS33",
  "bankIban": "GB82WEST12345698765432"
}
```

**Required Fields**:
- `bankAccountHolderName`: Account holder name (string)
- `bankAccountNumber`: Bank account number (string)
- `bankName`: Bank name (string)

**Optional Fields**:
- `bankSwiftCode`: SWIFT/BIC code (string)
- `bankIban`: IBAN if applicable (string)

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Bank details submitted successfully"
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}` (e.g., "Vendor is not using MoR model", validation errors)
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to submit bank details: <message>"}`

**Implementation Notes**:
- Only call this if `payoutModel === "MOR"`
- Validate IBAN format on frontend if provided
- Show success message after submission
- Bank details are required before requesting payouts
- After successful submission, user can request payouts

**Example Code**:
```javascript
const submitBankDetails = async (bankDetails) => {
  // Frontend validation
  if (!bankDetails.bankAccountHolderName || !bankDetails.bankAccountNumber || !bankDetails.bankName) {
    showError('Please fill in all required fields.');
    return { success: false, error: 'Missing required fields' };
  }
  
  // Validate IBAN format if provided
  if (bankDetails.bankIban && !isValidIBAN(bankDetails.bankIban)) {
    showError('Invalid IBAN format.');
    return { success: false, error: 'Invalid IBAN' };
  }
  
  try {
    const token = await getStoredToken();
    const response = await fetch('/vendors/mor/bank-details', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(bankDetails),
    });
    
    if (response.ok) {
      const result = await response.json();
      showMessage('Bank details submitted successfully!');
      // Navigate back or refresh payment screen
      navigation.goBack();
      return { success: true };
    } else {
      const error = await response.json();
      showError(error.error || 'Failed to submit bank details.');
      return { success: false, error: error.error };
    }
  } catch (error) {
    showError('Network error. Please try again.');
    return { success: false, error: error.message };
  }
};

// Simple IBAN validation (basic check)
const isValidIBAN = (iban) => {
  // Remove spaces and convert to uppercase
  const cleaned = iban.replace(/\s/g, '').toUpperCase();
  // Basic format check: 2 letters + 2 digits + up to 30 alphanumeric
  return /^[A-Z]{2}\d{2}[A-Z0-9]{4,30}$/.test(cleaned);
};
```

---

### 6. Get MoR Payouts

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
    "vendorId": 1,
    "amount": 5000,
    "currency": "EUR",
    "method": "BANK",
    "status": "COMPLETED",
    "requestedAt": "2024-01-14T10:00:00Z",
    "processedAt": "2024-01-15T14:30:00Z",
    "transactionReference": "TXN123456",
    "notes": "Processed via bank transfer"
  },
  {
    "id": 2,
    "vendorId": 1,
    "amount": 3000,
    "currency": "EUR",
    "method": "WISE",
    "status": "PENDING",
    "requestedAt": "2024-01-16T09:00:00Z",
    "processedAt": null,
    "transactionReference": null,
    "notes": null
  },
  {
    "id": 3,
    "vendorId": 1,
    "amount": 2000,
    "currency": "EUR",
    "method": "BANK",
    "status": "FAILED",
    "requestedAt": "2024-01-10T11:00:00Z",
    "processedAt": "2024-01-11T15:00:00Z",
    "transactionReference": null,
    "notes": "Insufficient funds in platform account"
  }
]
```

**Response Fields**:
- `id`: Payout ID (number)
- `vendorId`: Vendor ID (number)
- `amount`: Amount in cents (number)
- `currency`: Currency code (string)
- `method`: Payout method - `"BANK"`, `"WISE"`, `"OTHER"` (string)
- `status`: Payout status - `"PENDING"`, `"PROCESSING"`, `"COMPLETED"`, `"FAILED"` (string)
- `requestedAt`: ISO 8601 date-time when requested (string)
- `processedAt`: ISO 8601 date-time when processed (null if pending) (string or null)
- `transactionReference`: Bank transfer reference (string or null)
- `notes`: Admin notes (string or null)

**Error Responses**:
- **400 Bad Request**: `{"error": "Vendor is not using MoR model"}`
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to retrieve payouts: <message>"}`

**Implementation Notes**:
- Display in chronological order (newest first)
- Show status with color coding:
  - `COMPLETED`: Green
  - `PENDING`: Yellow/Orange
  - `PROCESSING`: Blue
  - `FAILED`: Red
- Format amounts: `(amount / 100).toFixed(2) + " " + currency.toUpperCase()`
- Show method badge (BANK, WISE, OTHER)
- Display processing time if available
- Show empty state if array is empty

**Example Code**:
```javascript
const getMoRPayouts = async () => {
  try {
    const token = await getStoredToken();
    const response = await fetch('/vendors/mor/payouts', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    if (response.ok) {
      const payouts = await response.json();
      
      // Format payouts for display
      const formattedPayouts = payouts.map(payout => ({
        ...payout,
        formattedAmount: `${(payout.amount / 100).toFixed(2)} ${payout.currency.toUpperCase()}`,
        statusColor: getStatusColor(payout.status),
        daysPending: payout.status === 'PENDING' || payout.status === 'PROCESSING' 
          ? Math.floor((new Date() - new Date(payout.requestedAt)) / (1000 * 60 * 60 * 24))
          : null,
      }));
      
      return { success: true, data: formattedPayouts };
    } else {
      const error = await response.json();
      return { success: false, error: error.error };
    }
  } catch (error) {
    return { success: false, error: error.message };
  }
};

const getStatusColor = (status) => {
  const colors = {
    'COMPLETED': '#4CAF50',
    'PENDING': '#FF9800',
    'PROCESSING': '#2196F3',
    'FAILED': '#F44336',
  };
  return colors[status] || '#757575';
};
```

---

### 7. Request MoR Payout

**Endpoint**: `POST /vendors/mor/request-payout`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "amount": 5000,
  "currency": "EUR",
  "description": "Monthly payout request"
}
```

**Required Fields**:
- `amount`: Amount in cents (number)

**Optional Fields**:
- `currency`: Currency code (default: "EUR") (string)
- `description`: Payout description (default: "Manual payout request") (string)

**Response** (200 OK):
```json
{
  "success": true,
  "payoutId": "payout_1",
  "message": "Payout request created successfully. It will be processed manually."
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "<error_message>"}` (e.g., "Insufficient balance", "Bank details not provided", "Vendor is not using MoR model")
- **401 Unauthorized**: Token expired
- **500 Internal Server Error**: `{"error": "Failed to request payout: <message>"}`

**Implementation Notes**:
- Only call this if `payoutModel === "MOR"`
- Check balance before allowing payout request
- Amount must be positive and not exceed available balance
- Bank details must be submitted before requesting payout
- Show confirmation dialog before submitting
- Payouts are processed manually by platform admins (may take 1-5 business days)
- Show success message with payout ID
- Refresh balance and payouts list after successful request

**Example Code**:
```javascript
const requestMoRPayout = async (amount, currency = 'EUR', description = 'Manual payout request') => {
  // Validate amount
  if (amount <= 0) {
    showError('Amount must be greater than zero.');
    return { success: false, error: 'Invalid amount' };
  }
  
  // Check balance first
  const balanceResult = await getMoRBalance();
  if (!balanceResult.success) {
    return { success: false, error: 'Failed to check balance' };
  }
  
  const availableBalance = balanceResult.data.balance;
  if (amount > availableBalance) {
    showError(`Insufficient balance. Available: ${(availableBalance / 100).toFixed(2)} ${balanceResult.data.currency.toUpperCase()}`);
    return { success: false, error: 'Insufficient balance' };
  }
  
  // Show confirmation dialog
  const confirmed = await showConfirmDialog(
    'Request Payout',
    `Request payout of ${(amount / 100).toFixed(2)} ${currency.toUpperCase()}? This will be processed manually within 1-5 business days.`,
  );
  
  if (!confirmed) {
    return { success: false, cancelled: true };
  }
  
  try {
    const token = await getStoredToken();
    const response = await fetch('/vendors/mor/request-payout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        amount, // Amount in cents
        currency,
        description,
      }),
    });
    
    if (response.ok) {
      const result = await response.json();
      showMessage(`Payout request created successfully! ID: ${result.payoutId}. It will be processed within 1-5 business days.`);
      
      // Refresh balance and payouts list
      await Promise.all([
        getMoRBalance(),
        getMoRPayouts(),
      ]);
      
      return { success: true, payoutId: result.payoutId };
    } else {
      const error = await response.json();
      showError(error.error || 'Failed to request payout.');
      return { success: false, error: error.error };
    }
  } catch (error) {
    showError('Network error. Please try again.');
    return { success: false, error: error.message };
  }
};
```

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

### Common Error Scenarios

1. **Vendor Not Using MoR Model**:
   ```json
   {"error": "Vendor is not using MoR model"}
   ```
   - **Solution**: Check `payoutModel` from payment status endpoint first
   - Only call MoR endpoints if `payoutModel === "MOR"`

2. **Bank Details Not Provided**:
   ```json
   {"error": "Bank details not provided. Please submit bank details first."}
   ```
   - **Solution**: Navigate user to bank details form
   - Submit bank details before requesting payouts

3. **Insufficient Balance**:
   ```json
   {"error": "Insufficient balance"}
   ```
   - **Solution**: Check balance before allowing payout request
   - Show available balance to user

4. **Token Expiration (401)**:
   ```javascript
   if (response.status === 401) {
     // Clear stored token
     await AsyncStorage.removeItem('authToken');
     // Redirect to login
     navigation.navigate('Login');
   }
   ```

5. **Network Errors**:
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
       showError('No internet connection');
     } else {
       showError(error.message);
     }
   }
   ```

---

## Workflow Examples

### Workflow 1: Initial MoR Payment Setup

```javascript
// 1. Check payment status
const statusResult = await checkPaymentStatus();

if (statusResult.isMoR) {
  // 2. Check if bank details are submitted
  const balanceResult = await getMoRBalance();
  
  if (!balanceResult.success && balanceResult.error.includes('Bank details')) {
    // 3. Navigate to bank details form
    navigation.navigate('BankDetailsForm');
  } else {
    // 4. Show payment dashboard with balance
    navigation.navigate('MoRPaymentDashboard', {
      balance: balanceResult.data,
    });
  }
}
```

### Workflow 2: Complete MoR Payment Flow

```javascript
// 1. Get payment status
const status = await checkPaymentStatus();

if (status.payoutModel === 'MOR') {
  // 2. Get balance
  const balance = await getMoRBalance();
  
  // 3. Get transactions
  const transactions = await getMoRTransactions(50);
  
  // 4. Get payouts
  const payouts = await getMoRPayouts();
  
  // 5. Display in dashboard
  setMoRData({
    balance: balance.data,
    transactions: transactions.data,
    payouts: payouts.data,
  });
}
```

### Workflow 3: Submit Bank Details and Request Payout

```javascript
// 1. Submit bank details
const bankDetailsResult = await submitBankDetails({
  bankAccountHolderName: 'John Doe',
  bankAccountNumber: '1234567890',
  bankName: 'Example Bank',
  bankSwiftCode: 'EXAMUS33',
  bankIban: 'GB82WEST12345698765432',
});

if (bankDetailsResult.success) {
  // 2. Check balance
  const balance = await getMoRBalance();
  
  if (balance.data.balance >= 5000) {
    // 3. Request payout
    const payoutResult = await requestMoRPayout(
      5000,
      'EUR',
      'Monthly payout request',
    );
    
    if (payoutResult.success) {
      // 4. Refresh data
      await refreshMoRData();
    }
  }
}
```

### Workflow 4: View MoR Payment History

```javascript
// 1. Get transactions
const transactionsResult = await getMoRTransactions(100);

if (transactionsResult.success) {
  // 2. Group by type
  const groupedTransactions = {
    payments: transactionsResult.data.filter(t => t.type === 'ORDER_PAYMENT'),
    payouts: transactionsResult.data.filter(t => t.type === 'PAYOUT'),
    adjustments: transactionsResult.data.filter(t => t.type === 'ADJUSTMENT'),
    refunds: transactionsResult.data.filter(t => t.type === 'REFUND'),
  };
  
  // 3. Display in history screen
  navigation.navigate('MoRTransactionHistory', {
    transactions: groupedTransactions,
  });
}
```

---

## Data Models

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

### MoR Balance Response

```typescript
interface MoRBalanceResponse {
  balance: number; // in cents
  currency: string;
  formattedBalance: string;
}
```

### MoR Transaction Response

```typescript
interface MoRTransaction {
  id: number;
  vendorId: number;
  amount: number; // in cents (positive for credits, negative for debits)
  currency: string;
  type: 'ORDER_PAYMENT' | 'PAYOUT' | 'ADJUSTMENT' | 'REFUND';
  description: string;
  orderId?: number | null;
  payoutId?: string | null;
  createdAt: string; // ISO 8601
}
```

### MoR Payout Response

```typescript
interface MoRPayout {
  id: number;
  vendorId: number;
  amount: number; // in cents
  currency: string;
  method: 'BANK' | 'WISE' | 'OTHER';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  requestedAt: string; // ISO 8601
  processedAt?: string | null; // ISO 8601
  transactionReference?: string | null;
  notes?: string | null;
}
```

### Bank Details Request

```typescript
interface BankDetailsRequest {
  bankAccountHolderName: string;
  bankAccountNumber: string;
  bankName: string;
  bankSwiftCode?: string;
  bankIban?: string;
}
```

### Payout Request

```typescript
interface PayoutRequest {
  amount: number; // in cents
  currency?: string; // default: "EUR"
  description?: string; // default: "Manual payout request"
}
```

---

## Testing Checklist

- [ ] Check payment status returns correct `payoutModel`
- [ ] Get MoR balance displays correctly
- [ ] Get MoR transactions shows all transaction types
- [ ] Submit bank details with all required fields
- [ ] Submit bank details with optional fields (SWIFT, IBAN)
- [ ] Get MoR payouts shows all payout statuses
- [ ] Request payout with valid amount
- [ ] Request payout with insufficient balance (error handling)
- [ ] Request payout without bank details (error handling)
- [ ] Error handling for expired tokens
- [ ] Error handling for network failures
- [ ] Error handling for non-MoR vendor accessing MoR endpoints
- [ ] Amount formatting (cents to currency)
- [ ] Date/time formatting (ISO 8601)
- [ ] Empty states shown when no data
- [ ] Loading states during API calls
- [ ] Refresh balance after payout request
- [ ] Refresh payouts list after payout request

---

## Additional Notes

1. **Amount Formatting**: MoR amounts are in cents. Always divide by 100 for display:
   ```javascript
   const formattedAmount = `${(amount / 100).toFixed(2)} ${currency.toUpperCase()}`;
   ```

2. **Payout Processing Time**: MoR payouts are processed manually by platform admins. Inform users that processing may take 1-5 business days.

3. **Balance Updates**: Balance is updated automatically when:
   - Customer makes a payment (credit)
   - Payout is processed (debit)
   - Platform makes adjustments (credit/debit)

4. **Bank Details Security**: Bank details are stored securely. Never log or expose them in error messages.

5. **Payout Model Check**: Always check `payoutModel` from payment status before calling MoR endpoints. If `payoutModel !== "MOR"`, use Stripe endpoints instead.

6. **Transaction Types**:
   - `ORDER_PAYMENT`: Customer payment (credit to vendor)
   - `PAYOUT`: Manual payout (debit from vendor)
   - `ADJUSTMENT`: Platform adjustments (can be credit or debit)
   - `REFUND`: Refund to customer (debit from vendor)

7. **Payout Status Flow**:
   - `PENDING`: Request submitted, waiting for admin processing
   - `PROCESSING`: Admin is processing the payout
   - `COMPLETED`: Payout successfully processed
   - `FAILED`: Payout failed (check notes for reason)

8. **Currency**: Default currency is "EUR", but can be different based on vendor's country.

---

## Support

For issues or questions:
1. Check error messages in API responses
2. Verify authentication token is valid
3. Ensure vendor has `payoutModel === "MOR"`
4. Check network connectivity
5. Review server logs if available
