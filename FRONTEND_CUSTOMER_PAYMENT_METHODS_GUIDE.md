# Customer Payment Method Management Integration Guide

This guide provides comprehensive instructions for integrating customer payment method management (cards and bank accounts) into the mobile application.

## Overview

The payment method management feature allows customers to:
- View all their registered payment methods (cards and bank accounts)
- Add new cards and bank accounts
- Set a default payment method
- Delete payment methods
- Manage payment methods securely through Stripe

## Base URL

All endpoints are under the `payment-service`:
- **Development**: `http://localhost:8086/payment`
- **Production**: `https://api.stillfresh.com/payment`

## Authentication

All endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

If the token expires (401 response), redirect the user to the login screen.

---

## API Endpoints

### 1. List All Payment Methods

**Endpoint**: `GET /payment/payment-methods`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
[
  {
    "paymentMethodId": "pm_1SVaf7BMOGvRra45SvXB2puN",
    "type": "card",
    "isDefault": true,
    "cardBrand": "visa",
    "cardLast4": "4242",
    "cardExpMonth": 12,
    "cardExpYear": 2025,
    "cardFunding": "credit",
    "country": "US",
    "currency": "usd",
    "bankAccountType": null,
    "bankAccountLast4": null,
    "bankName": null,
    "bankAccountHolderType": null,
    "bankAccountStatus": null
  },
  {
    "paymentMethodId": "pm_1SVaf8BMOGvRra45SvXB2puO",
    "type": "us_bank_account",
    "isDefault": false,
    "cardBrand": null,
    "cardLast4": null,
    "cardExpMonth": null,
    "cardExpYear": null,
    "cardFunding": null,
    "country": "US",
    "currency": "usd",
    "bankAccountType": "checking",
    "bankAccountLast4": "6789",
    "bankName": "Chase",
    "bankAccountHolderType": "individual",
    "bankAccountStatus": null
  }
]
```

**Response Fields**:
- `paymentMethodId` (string): Stripe payment method ID
- `type` (string): `"card"` or `"us_bank_account"`
- `isDefault` (boolean): Whether this is the default payment method
- **Card fields** (only for `type: "card"`):
  - `cardBrand`: Card brand (visa, mastercard, amex, etc.)
  - `cardLast4`: Last 4 digits of card number
  - `cardExpMonth`: Expiration month (1-12)
  - `cardExpYear`: Expiration year (4 digits)
  - `cardFunding`: Card funding type (credit, debit, prepaid, unknown)
- **Bank account fields** (only for `type: "us_bank_account"`):
  - `bankAccountType`: Account type (checking, savings)
  - `bankAccountLast4`: Last 4 digits of account number
  - `bankName`: Bank name
  - `bankAccountHolderType`: Holder type (individual, company)
  - `bankAccountStatus`: Status (typically null for PaymentMethod, managed via verification)
- **Common fields**:
  - `country`: Country code (e.g., "US")
  - `currency`: Currency code (e.g., "usd")

**Error Responses**:
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to retrieve payment methods"}`

**Implementation Notes**:
- Call this endpoint when customer opens "Payment Methods" screen
- Display cards and bank accounts in separate sections or unified list
- Show default badge/indicator for the default payment method
- Cache the list and refresh on pull-to-refresh
- Show empty state if list is empty with "Add Payment Method" button

---

### 2. Get Specific Payment Method

**Endpoint**: `GET /payment/payment-methods/{paymentMethodId}`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `paymentMethodId` (required): Stripe payment method ID (e.g., `pm_1SVaf7BMOGvRra45SvXB2puN`)

**Response** (200 OK):
```json
{
  "paymentMethodId": "pm_1SVaf7BMOGvRra45SvXB2puN",
  "type": "card",
  "isDefault": true,
  "cardBrand": "visa",
  "cardLast4": "4242",
  "cardExpMonth": 12,
  "cardExpYear": 2025,
  "cardFunding": "credit",
  "country": "US",
  "currency": "usd"
}
```

**Error Responses**:
- **401 Unauthorized**: Token expired - redirect to login
- **404 Not Found**: Payment method not found or doesn't belong to customer
- **500 Internal Server Error**: `{"error": "Failed to retrieve payment method"}`

**Implementation Notes**:
- Use this endpoint to get detailed information about a specific payment method
- Useful for showing payment method details in a detail screen
- Verify the payment method belongs to the customer (backend handles this)

---

### 3. Register Card

**Endpoint**: `POST /payment/register-card`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "paymentMethodId": "pm_1SVaf7BMOGvRra45SvXB2puN"
}
```

**Request Fields**:
- `paymentMethodId` (required): Payment method token from Stripe.js/Elements

**Response** (200 OK):
```json
{
  "customerId": "cus_TSVgExSn2LnWuv",
  "message": "Card registered successfully."
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Invalid payment method ID"}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to register card: <error_message>"}`

**Implementation Notes**:
- **CRITICAL**: You must use Stripe.js or Stripe Elements to collect card details securely
- **NEVER** send raw card numbers, CVV, or expiration dates to your backend
- Use Stripe's `createPaymentMethod` to get a payment method token
- Send only the token (`paymentMethodId`) to your backend
- After successful registration, refresh the payment methods list
- Show success message: "Card added successfully"

**Stripe Integration Requirements**:
- For React Native: Use `@stripe/stripe-react-native` package
- For Web: Use `@stripe/stripe-js` and `@stripe/react-stripe-js`
- Create payment method client-side before calling this endpoint

**Example Flow**:
1. User enters card details in Stripe Elements
2. Call `createPaymentMethod` from Stripe SDK
3. Receive `paymentMethod.id` (e.g., `pm_xxxxx`)
4. Call this endpoint with the payment method ID
5. Show success and refresh payment methods list

---

### 4. Register Bank Account

**Endpoint**: `POST /payment/register-bank-account`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/x-www-form-urlencoded
```

**Request Parameters**:
- `bankAccountToken` (required): Payment method token from Stripe.js/Elements for bank account

**Request Example**:
```
POST /payment/register-bank-account?bankAccountToken=pm_1SVaf8BMOGvRra45SvXB2puO
```

**Response** (200 OK):
```json
{
  "paymentMethodId": "pm_1SVaf8BMOGvRra45SvXB2puO",
  "type": "us_bank_account",
  "isDefault": false,
  "bankAccountType": "checking",
  "bankAccountLast4": "6789",
  "bankName": "Chase",
  "bankAccountHolderType": "individual",
  "country": "US",
  "currency": "usd"
}
```

**Error Responses**:
- **400 Bad Request**: `{"error": "Payment method is not a bank account"}` or `{"error": "Invalid bank account token"}`
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: `{"error": "Failed to register bank account: <error_message>"}`

**Implementation Notes**:
- **CRITICAL**: You must use Stripe.js or Stripe Elements to collect bank account details securely
- **NEVER** send raw account numbers, routing numbers, or account holder information to your backend
- Use Stripe's `createPaymentMethod` with type `us_bank_account` to get a token
- Send only the token to your backend
- After successful registration, refresh the payment methods list
- Show success message: "Bank account added successfully"
- Note: Bank account verification may be required by Stripe (micro-deposits)

**Stripe Integration Requirements**:
- For React Native: Use `@stripe/stripe-react-native` package with `BankAccount` component
- For Web: Use Stripe Elements with `BankAccountElement`
- Currently supports US bank accounts only (`us_bank_account` type)

**Example Flow**:
1. User enters bank account details in Stripe Elements
2. Call `createPaymentMethod` with type `us_bank_account` from Stripe SDK
3. Receive `paymentMethod.id` (e.g., `pm_xxxxx`)
4. Call this endpoint with the payment method ID as `bankAccountToken`
5. Show success and refresh payment methods list
6. Inform user about potential verification requirements

---

### 5. Set Default Payment Method

**Endpoint**: `PUT /payment/payment-methods/{paymentMethodId}/default`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `paymentMethodId` (required): Payment method ID to set as default

**Response** (200 OK):
```json
{
  "paymentMethodId": "pm_1SVaf7BMOGvRra45SvXB2puN",
  "type": "card",
  "isDefault": true,
  "cardBrand": "visa",
  "cardLast4": "4242",
  "cardExpMonth": 12,
  "cardExpYear": 2025,
  "cardFunding": "credit",
  "country": "US",
  "currency": "usd"
}
```

**Error Responses**:
- **401 Unauthorized**: Token expired - redirect to login
- **404 Not Found**: Payment method not found or doesn't belong to customer
- **500 Internal Server Error**: `{"error": "Failed to set default payment method"}`

**Implementation Notes**:
- Call this when user selects "Set as Default" option
- Update the UI immediately (optimistic update)
- Refresh payment methods list after successful update
- Show success message: "Default payment method updated"
- Only one payment method can be default at a time

---

### 6. Delete Payment Method

**Endpoint**: `DELETE /payment/payment-methods/{paymentMethodId}`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `paymentMethodId` (required): Payment method ID to delete

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Payment method deleted successfully"
}
```

**Error Responses**:
- **401 Unauthorized**: Token expired - redirect to login
- **404 Not Found**: Payment method not found or doesn't belong to customer
- **500 Internal Server Error**: `{"error": "Failed to delete payment method"}`

**Response** (404 Not Found):
```json
{
  "success": false,
  "message": "Payment method does not belong to this customer"
}
```

**Implementation Notes**:
- Show confirmation dialog before deleting: "Are you sure you want to remove this payment method?"
- If deleting the default payment method, inform user that default will be cleared
- Remove from UI immediately after successful deletion
- Refresh payment methods list
- Show success message: "Payment method removed"
- If it was the only payment method, show appropriate message

---

## UI/UX Recommendations

### Payment Methods List Screen

**Layout**:
- Header: "Payment Methods" with "Add" button
- Section 1: Default Payment Method (highlighted)
- Section 2: Other Payment Methods
- Empty state: "No payment methods" with "Add Payment Method" button

**Card Display**:
- Show card brand icon (Visa, Mastercard, Amex, etc.)
- Display: "**** **** **** 4242" (last 4 digits)
- Show expiration: "Expires 12/2025"
- Show "Default" badge if `isDefault: true`
- Action buttons: "Set as Default" (if not default), "Delete"

**Bank Account Display**:
- Show bank icon or name
- Display: "Chase •••• 6789" (bank name and last 4)
- Show account type: "Checking" or "Savings"
- Show "Default" badge if `isDefault: true`
- Action buttons: "Set as Default" (if not default), "Delete"

### Add Payment Method Screen

**Options**:
1. **Add Card**: Opens Stripe card input form
2. **Add Bank Account**: Opens Stripe bank account input form

**Flow**:
1. User selects payment method type
2. Stripe Elements form is displayed
3. User enters details (handled securely by Stripe)
4. On submit, create payment method via Stripe SDK
5. Call backend endpoint with payment method token
6. Show success message
7. Navigate back to payment methods list

### Error Handling

**Network Errors**:
- Show retry button
- Display user-friendly error message
- Log error for debugging

**Validation Errors**:
- Show inline validation errors
- Highlight invalid fields
- Provide helpful error messages

**Stripe Errors**:
- Handle Stripe-specific errors (card declined, invalid details, etc.)
- Show user-friendly messages
- Provide guidance on how to fix

---

## Security Best Practices

1. **Never Store Card Details**:
   - Never store card numbers, CVV, or expiration dates
   - Always use Stripe.js/Elements for card input
   - Only send payment method tokens to backend

2. **Token Handling**:
   - Payment method tokens are single-use
   - Create new token for each registration attempt
   - Don't cache or reuse tokens

3. **Authentication**:
   - Always include JWT token in requests
   - Handle token expiration gracefully
   - Refresh token if refresh mechanism exists

4. **Error Messages**:
   - Don't expose sensitive information in error messages
   - Show generic errors to users
   - Log detailed errors server-side only

---

## State Management

### Recommended State Structure

```typescript
interface PaymentMethodState {
  paymentMethods: CustomerPaymentMethodDto[];
  defaultPaymentMethodId: string | null;
  isLoading: boolean;
  error: string | null;
}
```

### Actions

- `fetchPaymentMethods()`: Load all payment methods
- `addPaymentMethod(token, type)`: Add new payment method
- `setDefaultPaymentMethod(id)`: Set default
- `deletePaymentMethod(id)`: Delete payment method
- `clearError()`: Clear error state

---

## Integration Flow Example

### Complete Flow: Adding a Card

1. **User navigates to Payment Methods screen**
   - Call `GET /payment/payment-methods`
   - Display list of existing payment methods

2. **User taps "Add Card"**
   - Navigate to "Add Card" screen
   - Initialize Stripe Elements with card input

3. **User enters card details**
   - Card number, expiration, CVV, ZIP
   - Stripe Elements handles validation

4. **User submits form**
   - Call Stripe SDK: `createPaymentMethod({ type: 'card', card: cardElement })`
   - Receive `paymentMethod.id`

5. **Call backend**
   - `POST /payment/register-card` with `{ paymentMethodId: "pm_xxxxx" }`
   - Show loading indicator

6. **Handle response**
   - On success: Show success message, refresh payment methods list, navigate back
   - On error: Show error message, allow retry

---

## Testing Guide

### Test Scenarios

1. **List Payment Methods**
   - Test with no payment methods (empty list)
   - Test with multiple cards
   - Test with cards and bank accounts
   - Test default payment method indicator

2. **Add Card**
   - Test valid card (Visa, Mastercard, Amex)
   - Test invalid card (declined, expired)
   - Test network errors
   - Test token expiration

3. **Add Bank Account**
   - Test valid US bank account
   - Test invalid account details
   - Test network errors

4. **Set Default**
   - Test setting default from non-default
   - Test changing default
   - Test with only one payment method

5. **Delete Payment Method**
   - Test deleting non-default payment method
   - Test deleting default payment method
   - Test deleting last payment method
   - Test confirmation dialog

6. **Error Handling**
   - Test 401 (token expired)
   - Test 404 (payment method not found)
   - Test 500 (server error)
   - Test network timeout

### Test Cards (Stripe Test Mode)

Use these test card numbers:
- **Success**: `4242 4242 4242 4242`
- **Decline**: `4000 0000 0000 0002`
- **Insufficient Funds**: `4000 0000 0000 9995`

Use any future expiration date and any 3-digit CVC.

---

## Dependencies

### Required Packages

**React Native**:
```json
{
  "@stripe/stripe-react-native": "^0.37.0"
}
```

**Web (if applicable)**:
```json
{
  "@stripe/stripe-js": "^2.0.0",
  "@stripe/react-stripe-js": "^2.0.0"
}
```

### Stripe Setup

1. Install Stripe SDK
2. Initialize Stripe with publishable key
3. Configure Stripe Elements
4. Handle payment method creation client-side

---

## API Service Example

```typescript
class PaymentMethodService {
  private baseUrl = 'http://localhost:8086/payment';
  
  async getPaymentMethods(token: string): Promise<CustomerPaymentMethodDto[]> {
    const response = await fetch(`${this.baseUrl}/payment-methods`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch payment methods');
    }
    
    return response.json();
  }
  
  async registerCard(token: string, paymentMethodId: string): Promise<CardRegistrationResponse> {
    const response = await fetch(`${this.baseUrl}/register-card`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ paymentMethodId })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to register card');
    }
    
    return response.json();
  }
  
  async registerBankAccount(token: string, bankAccountToken: string): Promise<CustomerPaymentMethodDto> {
    const response = await fetch(`${this.baseUrl}/register-bank-account?bankAccountToken=${bankAccountToken}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to register bank account');
    }
    
    return response.json();
  }
  
  async setDefaultPaymentMethod(token: string, paymentMethodId: string): Promise<CustomerPaymentMethodDto> {
    const response = await fetch(`${this.baseUrl}/payment-methods/${paymentMethodId}/default`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      throw new Error('Failed to set default payment method');
    }
    
    return response.json();
  }
  
  async deletePaymentMethod(token: string, paymentMethodId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/payment-methods/${paymentMethodId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to delete payment method');
    }
  }
}
```

---

## React Hook Example

```typescript
import { useState, useEffect } from 'react';
import { PaymentMethodService } from './services/PaymentMethodService';

export const usePaymentMethods = (token: string) => {
  const [paymentMethods, setPaymentMethods] = useState<CustomerPaymentMethodDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const service = new PaymentMethodService();
  
  const fetchPaymentMethods = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const methods = await service.getPaymentMethods(token);
      setPaymentMethods(methods);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load payment methods');
    } finally {
      setIsLoading(false);
    }
  };
  
  useEffect(() => {
    if (token) {
      fetchPaymentMethods();
    }
  }, [token]);
  
  const addCard = async (paymentMethodId: string) => {
    try {
      await service.registerCard(token, paymentMethodId);
      await fetchPaymentMethods(); // Refresh list
    } catch (err) {
      throw err;
    }
  };
  
  const addBankAccount = async (bankAccountToken: string) => {
    try {
      await service.registerBankAccount(token, bankAccountToken);
      await fetchPaymentMethods(); // Refresh list
    } catch (err) {
      throw err;
    }
  };
  
  const setDefault = async (paymentMethodId: string) => {
    try {
      await service.setDefaultPaymentMethod(token, paymentMethodId);
      await fetchPaymentMethods(); // Refresh list
    } catch (err) {
      throw err;
    }
  };
  
  const deleteMethod = async (paymentMethodId: string) => {
    try {
      await service.deletePaymentMethod(token, paymentMethodId);
      await fetchPaymentMethods(); // Refresh list
    } catch (err) {
      throw err;
    }
  };
  
  return {
    paymentMethods,
    isLoading,
    error,
    fetchPaymentMethods,
    addCard,
    addBankAccount,
    setDefault,
    deleteMethod
  };
};
```

---

## Summary

This integration enables customers to:
- ✅ View all payment methods (cards and bank accounts)
- ✅ Add new cards securely via Stripe
- ✅ Add new bank accounts securely via Stripe
- ✅ Set default payment method
- ✅ Delete payment methods
- ✅ Manage payment methods with proper security

**Key Points**:
- Always use Stripe.js/Elements for collecting payment details
- Never send raw card or bank account details to backend
- Handle authentication and token expiration
- Provide good UX with loading states and error handling
- Follow security best practices

For questions or issues, refer to the Stripe documentation or contact the backend team.

