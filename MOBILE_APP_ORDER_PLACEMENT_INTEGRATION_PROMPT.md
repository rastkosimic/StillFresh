# Mobile App AI Agent Prompt: Order Placement Integration

## Overview
You are an AI agent responsible for implementing the order placement functionality in the StillFresh mobile application. This guide explains how to integrate the complete order placement flow, including order creation, payment processing, and order status tracking.

**Key Concept**: Order placement is an **asynchronous process** that involves multiple backend services working together:
1. User submits order request
2. Backend validates offer availability
3. Payment is processed
4. Order is finalized and confirmed

The mobile app must handle this asynchronous flow gracefully, providing user feedback at each stage.

## Base API Configuration
- **Base URL**: `http://localhost:8080` (development) or your production API Gateway URL
- **Authentication**: JWT Bearer token required for all order endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

**⚠️ IMPORTANT**: All requests must go through the API Gateway, not directly to individual services.

## Authentication

Before placing orders, the user must be authenticated. Include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

If you receive a `401 Unauthorized` response, the token has expired. Redirect the user to the login screen.

---

## Order Placement Endpoint

### Place Order

**Endpoint**: `POST /orders/place-order`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "offerId": 123,
  "quantity": 2
}
```

**Request Fields**:
- `offerId` (number, required): The ID of the offer/product the user wants to order
- `quantity` (number, required): The number of items to order (must be > 0)

**Note**: The `userId` and `username` fields are automatically set by the backend from the JWT token. Do not include them in the request.

**Success Response (200 OK)**:
```json
{
  "message": "Order request submitted successfully."
}
```

**Error Responses**:
- **400 Bad Request**: 
  - `{"error": "Failed to place order: <error_message>"}`
  - Common errors:
    - "The selected offer is no longer active."
    - "The requested quantity exceeds available stock."
    - "The offer has expired."
    - "Invalid offer ID."
- **401 Unauthorized**: Token expired or invalid - redirect to login
- **500 Internal Server Error**: Server error - retry or show error message

**Implementation Notes**:
- This endpoint initiates the order placement process but does not immediately create the order
- The order is processed asynchronously through the backend services
- After successful submission, you should:
  1. Show a confirmation message to the user
  2. Navigate to order status/payment screen
  3. Monitor order status (see Order Status Tracking section)

---

## Order Placement Flow

The order placement process follows this asynchronous flow:

```
1. User submits order request
   ↓
2. Backend validates offer (availability, expiration, active status)
   ↓
3. Payment request is sent to payment service
   ↓
4. User completes payment (via payment methods)
   ↓
5. Payment is processed
   ↓
6. Order is finalized and confirmed
   ↓
7. User receives order confirmation
```

### Step-by-Step Flow

#### Step 1: Submit Order Request

Call `POST /orders/place-order` with `offerId` and `quantity`.

**Example**:
```typescript
const placeOrder = async (offerId: number, quantity: number) => {
  try {
    const response = await fetch('http://localhost:8080/orders/place-order', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        offerId,
        quantity
      })
    });

    if (response.status === 401) {
      // Token expired - redirect to login
      await logout();
      navigateToLogin();
      return;
    }

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to place order');
    }

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Order placement failed:', error);
    throw error;
  }
};
```

#### Step 2: Handle Order Submission Response

After successfully submitting the order request:

1. **Show confirmation message**: "Order request submitted successfully"
2. **Navigate to payment screen**: The user needs to complete payment
3. **Display order summary**: Show offer details, quantity, and total price

**UI Flow**:
```
Order Confirmation Screen
├── Order Summary
│   ├── Offer Name
│   ├── Quantity
│   ├── Unit Price
│   └── Total Price
├── Payment Method Selection
└── Complete Payment Button
```

#### Step 3: Payment Processing

After order submission, the backend automatically triggers payment processing. The user must complete payment using their saved payment methods.

**Refer to**: `MOBILE_APP_HYBRID_PAYMENT_INTEGRATION_PROMPT.md` for payment integration details.

**Key Points**:
- Payment is required before order is finalized
- Use the payment service endpoints to process payment
- Payment amount is calculated automatically by the backend (offer price × quantity)

#### Step 4: Order Status Tracking

After payment is processed, monitor the order status to confirm completion.

**See Order Status Tracking section below for details.**

---

## Order Status Tracking

### Get User Orders

**Endpoint**: `GET /orders`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters** (optional):
- `status` (string): Filter by order status (e.g., "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED")
- `limit` (number): Maximum number of orders to return (default: 50)
- `offset` (number): Number of orders to skip (for pagination)

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "offerId": 123,
    "userId": 456,
    "quantity": 2,
    "unitPrice": 1500.00,
    "totalPrice": 3000.00,
    "vendorId": 789,
    "currency": "RSD",
    "status": "CONFIRMED",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:35:00Z"
  }
]
```

**Response Fields**:
- `id` (number): Order ID
- `offerId` (number): ID of the offer/product
- `userId` (number): ID of the user who placed the order
- `quantity` (number): Number of items ordered
- `unitPrice` (number): Price per unit
- `totalPrice` (number): Total order price (unitPrice × quantity)
- `vendorId` (number): ID of the vendor offering the product
- `currency` (string): Currency code (e.g., "RSD", "EUR", "USD")
- `status` (string): Order status (see Order Statuses below)
- `createdAt` (string): ISO 8601 timestamp when order was created
- `updatedAt` (string): ISO 8601 timestamp when order was last updated

### Get Order by ID

**Endpoint**: `GET /orders/{orderId}`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response (200 OK)**:
```json
{
  "id": 1,
  "offerId": 123,
  "userId": 456,
  "quantity": 2,
  "unitPrice": 1500.00,
  "totalPrice": 3000.00,
  "vendorId": 789,
  "currency": "RSD",
  "status": "CONFIRMED",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

**Error Responses**:
- **404 Not Found**: Order not found or user doesn't have access
- **401 Unauthorized**: Token expired - redirect to login

### Order Statuses

| Status | Description | User Action |
|--------|-------------|-------------|
| `PENDING` | Order request submitted, awaiting payment | Complete payment |
| `CONFIRMED` | Payment processed, order confirmed | Wait for vendor processing |
| `PROCESSING` | Vendor is preparing the order | Wait |
| `READY` | Order is ready for pickup/delivery | Pick up or wait for delivery |
| `COMPLETED` | Order has been completed | Rate/review order |
| `CANCELLED` | Order was cancelled | Contact support if needed |

**Implementation Notes**:
- Poll order status after payment completion
- Show appropriate UI based on order status
- Update order status in real-time if possible (WebSocket/polling)

---

## Complete Implementation Example

### Order Service Module

```typescript
// services/orderService.ts
import { apiClient } from './apiClient';

export interface OrderRequest {
  offerId: number;
  quantity: number;
}

export interface Order {
  id: number;
  offerId: number;
  userId: number;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  vendorId: number;
  currency: string;
  status: 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'READY' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export const orderService = {
  /**
   * Place a new order
   */
  async placeOrder(request: OrderRequest): Promise<{ message: string }> {
    return apiClient.post('/orders/place-order', request);
  },

  /**
   * Get all orders for the current user
   */
  async getOrders(options?: {
    status?: string;
    limit?: number;
    offset?: number;
  }): Promise<Order[]> {
    const params = new URLSearchParams();
    if (options?.status) params.append('status', options.status);
    if (options?.limit) params.append('limit', options.limit.toString());
    if (options?.offset) params.append('offset', options.offset.toString());

    const queryString = params.toString();
    const endpoint = queryString ? `/orders?${queryString}` : '/orders';
    
    return apiClient.get<Order[]>(endpoint);
  },

  /**
   * Get a specific order by ID
   */
  async getOrder(orderId: number): Promise<Order> {
    return apiClient.get<Order>(`/orders/${orderId}`);
  }
};
```

### Order Placement Screen Component

```typescript
// screens/PlaceOrderScreen.tsx
import React, { useState } from 'react';
import { View, Text, Button, Alert, ActivityIndicator } from 'react-native';
import { orderService, OrderRequest } from '../services/orderService';
import { paymentService } from '../services/paymentService';

interface PlaceOrderScreenProps {
  offerId: number;
  offerName: string;
  offerPrice: number;
  availableQuantity: number;
  onOrderPlaced: (orderId: number) => void;
}

export function PlaceOrderScreen({
  offerId,
  offerName,
  offerPrice,
  availableQuantity,
  onOrderPlaced
}: PlaceOrderScreenProps) {
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(false);

  const handlePlaceOrder = async () => {
    // Validate quantity
    if (quantity <= 0) {
      Alert.alert('Error', 'Quantity must be greater than 0');
      return;
    }

    if (quantity > availableQuantity) {
      Alert.alert('Error', `Only ${availableQuantity} items available`);
      return;
    }

    setLoading(true);

    try {
      // Step 1: Submit order request
      const result = await orderService.placeOrder({
        offerId,
        quantity
      });

      Alert.alert('Success', result.message || 'Order request submitted successfully');

      // Step 2: Navigate to payment screen
      // The payment screen should handle payment processing
      // After payment, the order will be automatically finalized

      // Step 3: Navigate to order status screen
      // You can poll for order status or navigate to order details
      onOrderPlaced(offerId); // Navigate to order tracking

    } catch (error) {
      Alert.alert(
        'Order Failed',
        error instanceof Error ? error.message : 'Failed to place order. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  const totalPrice = offerPrice * quantity;

  return (
    <View style={{ padding: 20 }}>
      <Text style={{ fontSize: 20, fontWeight: 'bold', marginBottom: 10 }}>
        {offerName}
      </Text>
      
      <Text style={{ fontSize: 16, marginBottom: 5 }}>
        Price: {offerPrice.toFixed(2)} {currency}
      </Text>
      
      <Text style={{ fontSize: 16, marginBottom: 5 }}>
        Available: {availableQuantity}
      </Text>

      <View style={{ marginVertical: 20 }}>
        <Text style={{ fontSize: 16, marginBottom: 10 }}>Quantity:</Text>
        {/* Quantity selector component */}
        <QuantitySelector
          value={quantity}
          min={1}
          max={availableQuantity}
          onChange={setQuantity}
        />
      </View>

      <Text style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 20 }}>
        Total: {totalPrice.toFixed(2)} {currency}
      </Text>

      <Button
        title={loading ? 'Placing Order...' : 'Place Order'}
        onPress={handlePlaceOrder}
        disabled={loading || quantity <= 0}
      />

      {loading && <ActivityIndicator style={{ marginTop: 20 }} />}
    </View>
  );
}
```

### Order Status Tracking Component

```typescript
// screens/OrderStatusScreen.tsx
import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, RefreshControl, ActivityIndicator } from 'react-native';
import { orderService, Order } from '../services/orderService';

export function OrderStatusScreen() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadOrders = async () => {
    try {
      const orderList = await orderService.getOrders({ limit: 50 });
      setOrders(orderList);
    } catch (error) {
      console.error('Failed to load orders:', error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadOrders();
    
    // Poll for order updates every 5 seconds
    const interval = setInterval(loadOrders, 5000);
    return () => clearInterval(interval);
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    loadOrders();
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return '#FFA500';
      case 'CONFIRMED': return '#4CAF50';
      case 'PROCESSING': return '#2196F3';
      case 'READY': return '#9C27B0';
      case 'COMPLETED': return '#4CAF50';
      case 'CANCELLED': return '#F44336';
      default: return '#757575';
    }
  };

  if (loading) {
    return <ActivityIndicator style={{ flex: 1, justifyContent: 'center' }} />;
  }

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={orders}
        keyExtractor={(item) => item.id.toString()}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        renderItem={({ item }) => (
          <View style={{
            padding: 15,
            margin: 10,
            backgroundColor: '#fff',
            borderRadius: 8,
            borderLeftWidth: 4,
            borderLeftColor: getStatusColor(item.status)
          }}>
            <Text style={{ fontSize: 18, fontWeight: 'bold' }}>
              Order #{item.id}
            </Text>
            <Text style={{ fontSize: 14, color: '#666', marginTop: 5 }}>
              Quantity: {item.quantity}
            </Text>
            <Text style={{ fontSize: 16, fontWeight: 'bold', marginTop: 5 }}>
              Total: {item.totalPrice.toFixed(2)} {item.currency}
            </Text>
            <View style={{
              marginTop: 10,
              padding: 5,
              backgroundColor: getStatusColor(item.status) + '20',
              borderRadius: 4,
              alignSelf: 'flex-start'
            }}>
              <Text style={{
                color: getStatusColor(item.status),
                fontWeight: 'bold',
                textTransform: 'uppercase'
              }}>
                {item.status}
              </Text>
            </View>
            <Text style={{ fontSize: 12, color: '#999', marginTop: 5 }}>
              {new Date(item.createdAt).toLocaleString()}
            </Text>
          </View>
        )}
        ListEmptyComponent={
          <View style={{ padding: 20, alignItems: 'center' }}>
            <Text style={{ fontSize: 16, color: '#666' }}>
              No orders found
            </Text>
          </View>
        }
      />
    </View>
  );
}
```

---

## Error Handling

### Common Errors and Solutions

#### 1. "The selected offer is no longer active"
- **Cause**: Offer has been deactivated by the vendor
- **Solution**: Show error message and refresh offer list
- **User Action**: User should select a different offer

#### 2. "The requested quantity exceeds available stock"
- **Cause**: User requested more items than available
- **Solution**: Show available quantity and allow user to adjust
- **User Action**: Reduce quantity or wait for restock

#### 3. "The offer has expired"
- **Cause**: Offer expiration date has passed
- **Solution**: Show error message and refresh offer list
- **User Action**: User should select a different offer

#### 4. "Failed to place order: Network error"
- **Cause**: Network connectivity issue
- **Solution**: Retry with exponential backoff
- **User Action**: Check internet connection and try again

#### 5. 401 Unauthorized
- **Cause**: JWT token expired or invalid
- **Solution**: Clear token and redirect to login
- **User Action**: User must log in again

### Error Handling Example

```typescript
const handleOrderError = (error: Error) => {
  const errorMessage = error.message.toLowerCase();

  if (errorMessage.includes('no longer active')) {
    Alert.alert(
      'Offer Unavailable',
      'This offer is no longer available. Please select another offer.',
      [{ text: 'OK', onPress: () => refreshOffers() }]
    );
  } else if (errorMessage.includes('exceeds available stock')) {
    Alert.alert(
      'Insufficient Stock',
      'The requested quantity exceeds available stock. Please reduce the quantity.',
      [{ text: 'OK' }]
    );
  } else if (errorMessage.includes('expired')) {
    Alert.alert(
      'Offer Expired',
      'This offer has expired. Please select another offer.',
      [{ text: 'OK', onPress: () => refreshOffers() }]
    );
  } else if (errorMessage.includes('unauthorized')) {
    Alert.alert(
      'Session Expired',
      'Your session has expired. Please log in again.',
      [{ text: 'OK', onPress: () => logoutAndNavigateToLogin() }]
    );
  } else {
    Alert.alert(
      'Order Failed',
      'Failed to place order. Please try again.',
      [{ text: 'OK' }]
    );
  }
};
```

---

## UI/UX Guidelines

### Order Placement Screen

1. **Order Summary Section**:
   - Display offer name, image, description
   - Show unit price clearly
   - Display available quantity
   - Show total price calculation

2. **Quantity Selector**:
   - Use stepper or input field
   - Validate against available quantity
   - Update total price in real-time
   - Disable if quantity exceeds availability

3. **Place Order Button**:
   - Show loading state during submission
   - Disable during processing
   - Show success/error feedback

4. **Error Messages**:
   - Display inline errors for validation
   - Show alert dialogs for API errors
   - Provide actionable error messages

### Order Status Screen

1. **Order List**:
   - Group orders by status
   - Show most recent orders first
   - Display order details clearly
   - Use color coding for status

2. **Status Indicators**:
   - Use icons and colors for each status
   - Show status text clearly
   - Display timestamps

3. **Pull to Refresh**:
   - Allow users to manually refresh
   - Show loading indicator during refresh

4. **Order Details**:
   - Navigate to detailed view on tap
   - Show full order information
   - Display payment status

---

## Testing Checklist

### Order Placement
- [ ] User can select an offer and quantity
- [ ] Quantity validation works (min: 1, max: available)
- [ ] Order request is submitted successfully
- [ ] Success message is displayed
- [ ] User is navigated to payment screen
- [ ] Error handling works for all error cases
- [ ] Token expiration is handled correctly

### Order Status Tracking
- [ ] Orders list loads correctly
- [ ] Order status is displayed correctly
- [ ] Status colors/icons are correct
- [ ] Pull to refresh works
- [ ] Order details screen shows correct information
- [ ] Order status updates in real-time (polling)

### Error Scenarios
- [ ] "Offer no longer active" error is handled
- [ ] "Insufficient stock" error is handled
- [ ] "Offer expired" error is handled
- [ ] Network errors are handled gracefully
- [ ] 401 Unauthorized redirects to login
- [ ] Invalid offer ID shows appropriate error

### Edge Cases
- [ ] Order with quantity = 1 works
- [ ] Order with maximum available quantity works
- [ ] Order placement with expired token redirects to login
- [ ] Multiple rapid order submissions are handled
- [ ] Order status polling doesn't cause memory leaks

---

## Integration with Payment Flow

After placing an order, the user must complete payment. The order placement and payment flow should be integrated:

1. **Order Placement** → `POST /orders/place-order`
2. **Navigate to Payment Screen** → Show payment methods
3. **Process Payment** → Use payment service endpoints
4. **Payment Success** → Order is automatically finalized
5. **Order Confirmation** → Show order details and status

**Refer to**:
- `MOBILE_APP_HYBRID_PAYMENT_INTEGRATION_PROMPT.md` for payment integration
- `FRONTEND_CUSTOMER_PAYMENT_METHODS_GUIDE.md` for payment methods

---

## State Management Recommendations

```typescript
// Example state structure
interface OrderState {
  currentOrder: {
    offerId: number | null;
    quantity: number;
    loading: boolean;
    error: string | null;
  };
  orders: Order[];
  selectedOrder: Order | null;
  loading: boolean;
  error: string | null;
}
```

### State Management Example (React Context)

```typescript
// context/OrderContext.tsx
import React, { createContext, useContext, useState, useCallback } from 'react';
import { orderService, Order, OrderRequest } from '../services/orderService';

interface OrderContextType {
  placeOrder: (request: OrderRequest) => Promise<void>;
  orders: Order[];
  loading: boolean;
  error: string | null;
  refreshOrders: () => Promise<void>;
}

const OrderContext = createContext<OrderContextType | undefined>(undefined);

export function OrderProvider({ children }: { children: React.ReactNode }) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const placeOrder = useCallback(async (request: OrderRequest) => {
    setLoading(true);
    setError(null);
    try {
      await orderService.placeOrder(request);
      // Refresh orders after placing
      await refreshOrders();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to place order');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const refreshOrders = useCallback(async () => {
    try {
      const orderList = await orderService.getOrders();
      setOrders(orderList);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load orders');
    }
  }, []);

  return (
    <OrderContext.Provider
      value={{
        placeOrder,
        orders,
        loading,
        error,
        refreshOrders
      }}
    >
      {children}
    </OrderContext.Provider>
  );
}

export function useOrders() {
  const context = useContext(OrderContext);
  if (!context) {
    throw new Error('useOrders must be used within OrderProvider');
  }
  return context;
}
```

---

## Security Best Practices

1. **Token Storage**: Store JWT tokens securely (Keychain/SecureStorage)
2. **Input Validation**: Validate quantity and offerId on client side
3. **HTTPS Only**: Always use HTTPS in production
4. **Error Messages**: Don't expose sensitive backend details to users
5. **Rate Limiting**: Implement client-side rate limiting for order submissions
6. **Idempotency**: Consider implementing idempotency keys for order requests

---

## Performance Optimization

1. **Debounce Quantity Changes**: Debounce total price calculations
2. **Lazy Load Orders**: Implement pagination for order history
3. **Cache Order Data**: Cache recent orders to reduce API calls
4. **Optimistic Updates**: Show order as "pending" immediately after submission
5. **Polling Strategy**: Use exponential backoff for order status polling

---

## Summary

### Key Points

1. ✅ **Use API Gateway** (`http://localhost:8080` or production URL)
2. ✅ **Include JWT token** in Authorization header
3. ✅ **Handle asynchronous flow** - order placement triggers payment processing
4. ✅ **Validate input** - quantity must be > 0 and ≤ available stock
5. ✅ **Show user feedback** - loading states, success/error messages
6. ✅ **Track order status** - poll or use WebSocket for real-time updates
7. ✅ **Integrate with payment** - order must be paid before finalization
8. ✅ **Handle errors gracefully** - provide actionable error messages

### Quick Reference

**Order Placement**:
```
POST /orders/place-order
Body: { "offerId": number, "quantity": number }
```

**Get Orders**:
```
GET /orders?status=CONFIRMED&limit=50
```

**Get Order by ID**:
```
GET /orders/{orderId}
```

### Related Documents

- `FRONTEND_API_GATEWAY_INTEGRATION_GUIDE.md` - API Gateway setup
- `MOBILE_APP_HYBRID_PAYMENT_INTEGRATION_PROMPT.md` - Payment integration
- `FRONTEND_CUSTOMER_PAYMENT_METHODS_GUIDE.md` - Payment methods
- `ANDROID_GOOGLE_SIGNIN_IMPLEMENTATION.md` - Authentication

---

**Last Updated**: Based on API version as of implementation date
**API Base URL**: Configure based on environment (dev/staging/production)

