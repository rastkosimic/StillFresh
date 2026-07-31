# API Gateway Integration Guide for Mobile App

This guide provides comprehensive instructions for integrating the StillFresh backend API through the API Gateway service. **All API requests from the mobile app MUST go through the API Gateway**, not directly to individual services.

## Table of Contents
1. [Overview](#overview)
2. [Base Configuration](#base-configuration)
3. [Service Routes](#service-routes)
4. [Authentication](#authentication)
5. [Making Requests](#making-requests)
6. [Error Handling](#error-handling)
7. [Service-Specific Endpoints](#service-specific-endpoints)
8. [Best Practices](#best-practices)
9. [Code Examples](#code-examples)

---

## Overview

### What is API Gateway?

The API Gateway is a single entry point for all backend services. Instead of calling individual services directly, your mobile app should make all requests through the gateway.

**Benefits**:
- ✅ Single base URL for all services
- ✅ Service discovery and load balancing
- ✅ Centralized authentication
- ✅ Simplified network configuration
- ✅ Easier to handle service failures
- ✅ Consistent error handling

### Architecture

```
Mobile App
    ↓
API Gateway (Port 8080)
    ↓
    ├──→ Authorization Service (/auth/**)
    ├──→ User Service (/users/**)
    ├──→ Vendor Service (/vendors/**)
    ├──→ Offer Service (/offers/**)
    ├──→ Order Service (/orders/**)
    └──→ Payment Service (/payment/**)
```

---

## Base Configuration

### Base URL

**Development**:
```
http://localhost:8080
```

**Production**:
```
https://api.stillfresh.com
```

**⚠️ IMPORTANT**: Always use the API Gateway URL, never call services directly!

### Content-Type

All requests should use:
```
Content-Type: application/json
```

---

## Service Routes

The API Gateway routes requests to different services based on URL paths:

| Path Prefix | Service | Description |
|------------|---------|-------------|
| `/auth/**` | Authorization Service | Authentication, login, registration, JWT tokens |
| `/users/**` | User Service | User profile management, user data |
| `/vendors/**` | Vendor Service | Vendor profiles, payment accounts, Stripe Connect, MoR |
| `/offers/**` | Offer Service | Product offers, menu items |
| `/orders/**` | Order Service | Order creation, order history, order status |
| `/payment/**` | Payment Service | Payment processing, payment methods, Stripe payments |

### Route Examples

Instead of calling services directly:
```
❌ http://localhost:8081/users/profile
❌ http://localhost:8083/vendors/payment/status
❌ http://localhost:8086/payment/register-card
```

Use the API Gateway:
```
✅ http://localhost:8080/users/profile
✅ http://localhost:8080/vendors/payment/status
✅ http://localhost:8080/payment/register-card
```

---

## Authentication

### JWT Token

Most endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

### Getting a JWT Token

**1. User Registration**:
```
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "username": "user123",
  "email": "user@example.com",
  "password": "securePassword123",
  "role": "CUSTOMER"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "user123",
  "role": "CUSTOMER"
}
```

**2. User Login**:
```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "user123",
  "password": "securePassword123"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "user123",
  "role": "CUSTOMER"
}
```

### Token Storage

**React Native**:
```typescript
import * as SecureStore from 'expo-secure-store';

// Store token
await SecureStore.setItemAsync('jwt_token', token);

// Retrieve token
const token = await SecureStore.getItemAsync('jwt_token');

// Delete token (on logout)
await SecureStore.deleteItemAsync('jwt_token');
```

**iOS (Swift)**:
```swift
import KeychainSwift

let keychain = KeychainSwift()

// Store token
keychain.set(token, forKey: "jwt_token")

// Retrieve token
let token = keychain.get("jwt_token")

// Delete token
keychain.delete("jwt_token")
```

**Android (Kotlin)**:
```kotlin
import android.content.Context
import android.content.SharedPreferences

val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

// Store token
prefs.edit().putString("jwt_token", token).apply()

// Retrieve token
val token = prefs.getString("jwt_token", null)

// Delete token
prefs.edit().remove("jwt_token").apply()
```

### Token Expiration

- Tokens expire after a set period
- If you receive a `401 Unauthorized` response, the token has expired
- Redirect user to login screen
- Implement token refresh if available

---

## Making Requests

### Request Structure

All requests should follow this pattern:

```
<METHOD> <GATEWAY_URL><SERVICE_PATH>
Headers:
  Authorization: Bearer <jwt_token>
  Content-Type: application/json
Body: <json_data> (for POST/PUT requests)
```

### Example: Get User Profile

```typescript
const response = await fetch('http://localhost:8080/users/profile', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const userData = await response.json();
```

### Example: Create Order

```typescript
const response = await fetch('http://localhost:8080/orders', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    offerId: 1,
    quantity: 2,
    deliveryAddress: "123 Main St"
  })
});

const order = await response.json();
```

---

## Error Handling

### HTTP Status Codes

| Status Code | Meaning | Action |
|------------|---------|--------|
| 200 | Success | Process response data |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Check request body/parameters |
| 401 | Unauthorized | Token expired/invalid - redirect to login |
| 403 | Forbidden | User doesn't have permission |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | Retry or show error message |

### Error Response Format

Most errors return JSON in this format:

```json
{
  "error": "Error message here",
  "errorCode": "ERROR_CODE",
  "timestamp": "2025-11-21T12:00:00Z"
}
```

### Error Handling Example

```typescript
async function makeRequest(url: string, options: RequestInit) {
  try {
    const response = await fetch(url, options);
    
    if (response.status === 401) {
      // Token expired - redirect to login
      await logout();
      navigateToLogin();
      return;
    }
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Request failed');
    }
    
    return await response.json();
  } catch (error) {
    console.error('Request failed:', error);
    throw error;
  }
}
```

---

## Service-Specific Endpoints

### Authorization Service (`/auth/**`)

**Base Path**: `http://localhost:8080/auth`

**Key Endpoints**:
- `POST /auth/register` - Register new user
- `POST /auth/login` - User login
- `POST /auth/logout` - User logout
- `POST /auth/refresh` - Refresh JWT token (if available)

**Example**:
```typescript
// Login
const response = await fetch('http://localhost:8080/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'user123', password: 'pass123' })
});
```

### User Service (`/users/**`)

**Base Path**: `http://localhost:8080/users`

**Key Endpoints**:
- `GET /users/profile` - Get user profile
- `PUT /users/profile` - Update user profile
- `GET /users/{id}` - Get user by ID

**Example**:
```typescript
// Get profile
const profile = await fetch('http://localhost:8080/users/profile', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### Vendor Service (`/vendors/**`)

**Base Path**: `http://localhost:8080/vendors`

**Key Endpoints**:
- `GET /vendors/profile` - Get vendor profile
- `PUT /vendors/profile` - Update vendor profile
- `GET /vendors/payment/status` - Get payment account status
- `POST /vendors/payment/onboarding-link` - Get Stripe onboarding link
- `GET /vendors/payment/balance` - Get MoR balance (for MoR vendors)
- `GET /vendors/payment/transactions` - Get MoR transactions
- `POST /vendors/payment/payouts` - Request MoR payout

**Example**:
```typescript
// Get payment status
const status = await fetch('http://localhost:8080/vendors/payment/status', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### Offer Service (`/offers/**`)

**Base Path**: `http://localhost:8080/offers`

**Key Endpoints**:
- `GET /offers` - List all offers
- `GET /offers/{id}` - Get offer by ID
- `POST /offers` - Create offer (vendor only)
- `PUT /offers/{id}` - Update offer (vendor only)
- `DELETE /offers/{id}` - Delete offer (vendor only)

**Example**:
```typescript
// List offers
const offers = await fetch('http://localhost:8080/offers', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### Order Service (`/orders/**`)

**Base Path**: `http://localhost:8080/orders`

**Key Endpoints**:
- `POST /orders` - Create new order
- `GET /orders` - Get user's orders
- `GET /orders/{id}` - Get order by ID
- `PUT /orders/{id}/status` - Update order status

**Example**:
```typescript
// Create order
const order = await fetch('http://localhost:8080/orders', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    offerId: 1,
    quantity: 2
  })
});
```

### Payment Service (`/payment/**`)

**Base Path**: `http://localhost:8080/payment`

**Key Endpoints**:
- `POST /payment/register-card` - Register a card
- `POST /payment/register-bank-account` - Register a bank account
- `GET /payment/payment-methods` - List payment methods
- `GET /payment/payment-methods/{id}` - Get payment method
- `PUT /payment/payment-methods/{id}/default` - Set default payment method
- `DELETE /payment/payment-methods/{id}` - Delete payment method
- `POST /payment/charge` - Make a payment

**Example**:
```typescript
// Register card
const response = await fetch('http://localhost:8080/payment/register-card', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    paymentMethodId: 'pm_xxxxx' // From Stripe.js
  })
});
```

---

## Best Practices

### 1. Use a Single Base URL

Create a configuration file:

```typescript
// config/api.ts
export const API_CONFIG = {
  BASE_URL: __DEV__ 
    ? 'http://localhost:8080' 
    : 'https://api.stillfresh.com',
  TIMEOUT: 30000 // 30 seconds
};
```

### 2. Create a Centralized API Client

```typescript
// services/apiClient.ts
import { API_CONFIG } from '../config/api';
import * as SecureStore from 'expo-secure-store';

class ApiClient {
  private baseUrl: string;

  constructor() {
    this.baseUrl = API_CONFIG.BASE_URL;
  }

  private async getToken(): Promise<string | null> {
    return await SecureStore.getItemAsync('jwt_token');
  }

  async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = await this.getToken();
    
    const url = `${this.baseUrl}${endpoint}`;
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      // Token expired
      await SecureStore.deleteItemAsync('jwt_token');
      throw new Error('Unauthorized');
    }

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.error || `HTTP ${response.status}`);
    }

    return response.json();
  }

  get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  post<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
```

### 3. Use Service-Specific Modules

```typescript
// services/authService.ts
import { apiClient } from './apiClient';

export const authService = {
  login: async (username: string, password: string) => {
    return apiClient.post('/auth/login', { username, password });
  },
  
  register: async (userData: any) => {
    return apiClient.post('/auth/register', userData);
  },
  
  logout: async () => {
    return apiClient.post('/auth/logout');
  }
};
```

```typescript
// services/paymentService.ts
import { apiClient } from './apiClient';

export const paymentService = {
  registerCard: async (paymentMethodId: string) => {
    return apiClient.post('/payment/register-card', { paymentMethodId });
  },
  
  getPaymentMethods: async () => {
    return apiClient.get('/payment/payment-methods');
  },
  
  setDefaultPaymentMethod: async (paymentMethodId: string) => {
    return apiClient.put(`/payment/payment-methods/${paymentMethodId}/default`);
  }
};
```

### 4. Handle Network Errors

```typescript
async function makeRequestWithRetry<T>(
  requestFn: () => Promise<T>,
  maxRetries = 3
): Promise<T> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await requestFn();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      
      // Wait before retry (exponential backoff)
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, i) * 1000));
    }
  }
  throw new Error('Max retries exceeded');
}
```

### 5. Implement Request Interceptors

```typescript
// Add request logging, error handling, etc.
class ApiClient {
  // ... existing code ...
  
  async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    // Log request
    console.log(`[API] ${options.method || 'GET'} ${endpoint}`);
    
    try {
      const result = await this.makeRequest<T>(endpoint, options);
      console.log(`[API] Success: ${endpoint}`);
      return result;
    } catch (error) {
      console.error(`[API] Error: ${endpoint}`, error);
      throw error;
    }
  }
}
```

---

## Code Examples

### Complete Authentication Flow

```typescript
// services/authService.ts
import { apiClient } from './apiClient';
import * as SecureStore from 'expo-secure-store';

export const authService = {
  async login(username: string, password: string) {
    const response = await apiClient.post<{
      token: string;
      username: string;
      role: string;
    }>('/auth/login', { username, password });
    
    // Store token
    await SecureStore.setItemAsync('jwt_token', response.token);
    
    return response;
  },
  
  async register(userData: {
    username: string;
    email: string;
    password: string;
    role: 'CUSTOMER' | 'VENDOR';
  }) {
    const response = await apiClient.post<{
      token: string;
      username: string;
      role: string;
    }>('/auth/register', userData);
    
    // Store token
    await SecureStore.setItemAsync('jwt_token', response.token);
    
    return response;
  },
  
  async logout() {
    await SecureStore.deleteItemAsync('jwt_token');
    await apiClient.post('/auth/logout');
  },
  
  async isAuthenticated(): Promise<boolean> {
    const token = await SecureStore.getItemAsync('jwt_token');
    return token !== null;
  }
};
```

### Complete Payment Flow

```typescript
// services/paymentService.ts
import { apiClient } from './apiClient';

export interface PaymentMethod {
  paymentMethodId: string;
  type: 'card' | 'us_bank_account';
  isDefault: boolean;
  cardBrand?: string;
  cardLast4?: string;
  // ... other fields
}

export const paymentService = {
  async registerCard(paymentMethodId: string) {
    return apiClient.post('/payment/register-card', { paymentMethodId });
  },
  
  async getPaymentMethods(): Promise<PaymentMethod[]> {
    return apiClient.get<PaymentMethod[]>('/payment/payment-methods');
  },
  
  async setDefaultPaymentMethod(paymentMethodId: string): Promise<PaymentMethod> {
    return apiClient.put<PaymentMethod>(
      `/payment/payment-methods/${paymentMethodId}/default`
    );
  },
  
  async deletePaymentMethod(paymentMethodId: string): Promise<void> {
    return apiClient.delete(`/payment/payment-methods/${paymentMethodId}`);
  }
};
```

### React Hook Example

```typescript
// hooks/useApi.ts
import { useState, useEffect } from 'react';
import { apiClient } from '../services/apiClient';

export function useApi<T>(endpoint: string, options?: RequestInit) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchData() {
      try {
        setLoading(true);
        setError(null);
        const result = await apiClient.request<T>(endpoint, options);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [endpoint]);

  return { data, loading, error, refetch: () => fetchData() };
}
```

### Usage in Component

```typescript
// components/PaymentMethodsList.tsx
import { useApi } from '../hooks/useApi';
import { paymentService } from '../services/paymentService';

export function PaymentMethodsList() {
  const { data: paymentMethods, loading, error, refetch } = useApi(
    '/payment/payment-methods'
  );

  const handleSetDefault = async (paymentMethodId: string) => {
    try {
      await paymentService.setDefaultPaymentMethod(paymentMethodId);
      refetch(); // Refresh list
    } catch (error) {
      console.error('Failed to set default:', error);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <View>
      {paymentMethods?.map(pm => (
        <PaymentMethodCard
          key={pm.paymentMethodId}
          paymentMethod={pm}
          onSetDefault={() => handleSetDefault(pm.paymentMethodId)}
        />
      ))}
    </View>
  );
}
```

---

## Testing

### Development Environment

1. **Start all services** using Docker Compose:
   ```bash
   docker compose up
   ```

2. **Verify API Gateway is running**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. **Test authentication**:
   ```bash
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"testpass"}'
   ```

### Common Issues

**Issue**: Connection refused
- **Solution**: Ensure API Gateway is running on port 8080
- **Check**: `docker ps` to see if `api-gateway` container is running

**Issue**: 401 Unauthorized
- **Solution**: Token expired or invalid - get new token via login
- **Check**: Verify token is included in Authorization header

**Issue**: 404 Not Found
- **Solution**: Check that the path matches the service route
- **Check**: Verify service is registered in Eureka

**Issue**: 500 Internal Server Error
- **Solution**: Backend service error - check logs
- **Check**: Verify all services are running and healthy

---

## Summary

### Key Points

1. ✅ **Always use API Gateway** (`http://localhost:8080` or production URL)
2. ✅ **Never call services directly** - use gateway routes
3. ✅ **Include JWT token** in Authorization header for authenticated requests
4. ✅ **Handle errors gracefully** - especially 401 (token expired)
5. ✅ **Use centralized API client** for consistency
6. ✅ **Store tokens securely** using SecureStore/Keychain

### Service Routes Summary

| Service | Gateway Path | Direct Service (Don't Use) |
|---------|-------------|----------------------------|
| Auth | `/auth/**` | `http://localhost:8082` |
| Users | `/users/**` | `http://localhost:8081` |
| Vendors | `/vendors/**` | `http://localhost:8083` |
| Offers | `/offers/**` | `http://localhost:8084` |
| Orders | `/orders/**` | `http://localhost:8085` |
| Payment | `/payment/**` | `http://localhost:8086` |

### Quick Reference

```typescript
// Base URL
const BASE_URL = 'http://localhost:8080'; // or production URL

// All requests go through gateway
const endpoints = {
  login: `${BASE_URL}/auth/login`,
  userProfile: `${BASE_URL}/users/profile`,
  vendorStatus: `${BASE_URL}/vendors/payment/status`,
  offers: `${BASE_URL}/offers`,
  orders: `${BASE_URL}/orders`,
  paymentMethods: `${BASE_URL}/payment/payment-methods`
};
```

---

For detailed endpoint documentation, refer to:
- `FRONTEND_STRIPE_ENDPOINTS_GUIDE.md` - Stripe Connect endpoints
- `FRONTEND_MOR_INTEGRATION_GUIDE.md` - MoR payment endpoints
- `FRONTEND_CUSTOMER_PAYMENT_METHODS_GUIDE.md` - Customer payment methods






