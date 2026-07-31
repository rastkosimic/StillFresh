# Mobile App AI Agent Prompt: Order Cancellation and Rejection Integration

## Overview
You are an AI agent responsible for implementing order cancellation and rejection functionality in the StillFresh mobile application. This guide explains how to integrate customer order cancellation and vendor order rejection features, including push notifications, offer quantity restoration, and payment handling.

**Key Concept**: Both customers and vendors can cancel/reject orders. When an order is cancelled:
1. Order status changes to `CANCELLED`
2. Offer quantity is automatically restored (increased by cancelled quantity)
3. Payment authorization is released (if payment was held)
4. The other party receives a push notification

## Base API Configuration
- **Base URL**: `http://localhost:8080` (development) or your production API Gateway URL
- **Authentication**: JWT Bearer token required for all order endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

**⚠️ IMPORTANT**: All requests must go through the API Gateway, not directly to individual services.

## Authentication

Before cancelling or rejecting orders, the user must be authenticated. Include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

If you receive a `401 Unauthorized` response, the token has expired. Redirect the user to the login screen.

---

## Customer Order Cancellation

### Cancel Order Endpoint

**Endpoint**: `PUT /orders/{orderId}/cancel`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Path Parameters**:
- `orderId` (number, required): The ID of the order to cancel

**Request Body** (optional):
```json
{
  "reason": "Changed my mind"
}
```

**Request Fields**:
- `reason` (string, optional): Reason for cancellation (can be null)

**Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Order cancelled successfully"
}
```

**Error Responses**:
- **400 Bad Request**: 
  - `{"success": false, "message": "Failed to cancel order"}`
  - Common errors:
    - "Order not found"
    - "Order is already cancelled"
    - "Cannot cancel order - it is already completed"
- **401 Unauthorized**: Token expired or invalid - redirect to login
- **403 Forbidden**: User doesn't have permission to cancel this order
- **500 Internal Server Error**: Server error - retry or show error message

**Implementation Notes**:
- Only the customer who placed the order can cancel it
- Orders with status `COMPLETED` cannot be cancelled
- Orders with status `CANCELLED` cannot be cancelled again
- Payment authorization (if exists) is automatically released
- Offer quantity is automatically restored
- Vendor receives a push notification about the cancellation

---

## Vendor Order Rejection

### Reject Order Endpoint

**Endpoint**: `PUT /orders/{orderId}/reject`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Path Parameters**:
- `orderId` (number, required): The ID of the order to reject

**Request Body** (optional):
```json
{
  "reason": "Out of stock"
}
```

**Request Fields**:
- `reason` (string, optional): Reason for rejection (can be null)

**Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Order rejected successfully"
}
```

**Error Responses**:
- **400 Bad Request**: 
  - `{"success": false, "message": "Failed to reject order"}`
  - Common errors:
    - "Order not found"
    - "Order is already cancelled"
    - "Cannot reject order - it is already completed"
- **401 Unauthorized**: Token expired or invalid - redirect to login
- **403 Forbidden**: Vendor doesn't have permission to reject this order (not their order)
- **500 Internal Server Error**: Server error - retry or show error message

**Implementation Notes**:
- Only the vendor who owns the order can reject it
- Orders with status `COMPLETED` cannot be rejected
- Orders with status `CANCELLED` cannot be rejected again
- Payment authorization (if exists) is automatically released
- Offer quantity is automatically restored
- Customer receives a push notification about the rejection

---

## Push Notifications

### Notification System

The system automatically sends push notifications via Firebase Cloud Messaging (FCM) when orders are cancelled or rejected.

**Notification Types**:
- `ORDER_CANCELLED` - Sent when an order is cancelled or rejected

### Customer Cancellation Notification (to Vendor)

When a customer cancels an order, the vendor receives a push notification:

**Notification Data**:
```json
{
  "title": "Order Cancelled by Customer",
  "body": "Order #123 has been cancelled by the customer. Quantity: 2, Total: $30.00",
  "data": {
    "orderId": "123",
    "offerId": "456",
    "quantity": "2",
    "totalPrice": "30.00",
    "cancelledBy": "CUSTOMER",
    "type": "ORDER_CANCELLED"
  }
}
```

### Vendor Rejection Notification (to Customer)

When a vendor rejects an order, the customer receives a push notification:

**Notification Data**:
```json
{
  "title": "Order Rejected by Vendor",
  "body": "Your order #123 has been rejected by the vendor. Reason: Out of stock",
  "data": {
    "orderId": "123",
    "offerId": "456",
    "quantity": "2",
    "totalPrice": "30.00",
    "cancelledBy": "VENDOR",
    "reason": "Out of stock",
    "type": "ORDER_CANCELLED"
  }
}
```

### Handling Push Notifications

**React Native (Expo)**:
```typescript
import * as Notifications from 'expo-notifications';

// Configure notification handler
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

// Listen for notifications
useEffect(() => {
  const subscription = Notifications.addNotificationReceivedListener(notification => {
    const data = notification.request.content.data;
    
    if (data.type === 'ORDER_CANCELLED') {
      // Handle order cancellation notification
      if (data.cancelledBy === 'CUSTOMER') {
        // Vendor received notification - customer cancelled
        showAlert('Order Cancelled', `Order #${data.orderId} was cancelled by the customer`);
        // Refresh vendor's order list
        refreshOrders();
      } else if (data.cancelledBy === 'VENDOR') {
        // Customer received notification - vendor rejected
        showAlert('Order Rejected', `Your order #${data.orderId} was rejected. ${data.reason ? 'Reason: ' + data.reason : ''}`);
        // Refresh customer's order list
        refreshOrders();
      }
    }
  });

  return () => subscription.remove();
}, []);
```

**Android (Kotlin)**:
```kotlin
class OrderNotificationHandler : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val type = data["type"]
        
        if (type == "ORDER_CANCELLED") {
            val cancelledBy = data["cancelledBy"]
            val orderId = data["orderId"]
            
            if (cancelledBy == "CUSTOMER") {
                // Vendor received notification
                showNotification(
                    "Order Cancelled by Customer",
                    "Order #$orderId was cancelled by the customer"
                )
                // Refresh vendor's order list
                refreshOrders()
            } else if (cancelledBy == "VENDOR") {
                // Customer received notification
                val reason = data["reason"] ?: ""
                showNotification(
                    "Order Rejected by Vendor",
                    "Your order #$orderId was rejected. $reason"
                )
                // Refresh customer's order list
                refreshOrders()
            }
        }
    }
}
```

---

## Implementation Examples

### Order Service Module Extension

```typescript
// services/orderService.ts
import { apiClient } from './apiClient';

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
  paymentIntentId?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CancelOrderRequest {
  reason?: string;
}

export interface RejectOrderRequest {
  reason?: string;
}

export const orderService = {
  // ... existing methods ...

  /**
   * Cancel order (customer only)
   */
  async cancelOrder(orderId: number, reason?: string): Promise<{ success: boolean; message: string }> {
    return apiClient.put(`/orders/${orderId}/cancel`, reason ? { reason } : {});
  },

  /**
   * Reject order (vendor only)
   */
  async rejectOrder(orderId: number, reason?: string): Promise<{ success: boolean; message: string }> {
    return apiClient.put(`/orders/${orderId}/reject`, reason ? { reason } : {});
  }
};
```

### Customer Order Cancellation Screen

```typescript
// screens/CustomerOrderCancellationScreen.tsx
import React, { useState } from 'react';
import { View, Text, TextInput, Button, Alert, ActivityIndicator } from 'react-native';
import { orderService } from '../services/orderService';

interface CustomerOrderCancellationScreenProps {
  orderId: number;
  orderDetails: {
    quantity: number;
    totalPrice: number;
    currency: string;
    status: string;
  };
  onCancelled: () => void;
  onCancel: () => void;
}

export function CustomerOrderCancellationScreen({
  orderId,
  orderDetails,
  onCancelled,
  onCancel
}: CustomerOrderCancellationScreenProps) {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);

  const handleCancel = async () => {
    // Check if order can be cancelled
    if (orderDetails.status === 'COMPLETED') {
      Alert.alert('Cannot Cancel', 'This order has already been completed and cannot be cancelled.');
      return;
    }

    if (orderDetails.status === 'CANCELLED') {
      Alert.alert('Already Cancelled', 'This order has already been cancelled.');
      return;
    }

    Alert.alert(
      'Cancel Order',
      'Are you sure you want to cancel this order? The payment authorization will be released.',
      [
        { text: 'No', style: 'cancel', onPress: onCancel },
        {
          text: 'Yes, Cancel',
          style: 'destructive',
          onPress: async () => {
            setLoading(true);
            try {
              const result = await orderService.cancelOrder(orderId, reason || undefined);
              
              if (result.success) {
                Alert.alert(
                  'Order Cancelled',
                  'Your order has been cancelled successfully. The payment authorization has been released.',
                  [{ text: 'OK', onPress: onCancelled }]
                );
              } else {
                Alert.alert('Error', result.message || 'Failed to cancel order');
              }
            } catch (error) {
              Alert.alert(
                'Error',
                error instanceof Error ? error.message : 'Failed to cancel order. Please try again.'
              );
            } finally {
              setLoading(false);
            }
          }
        }
      ]
    );
  };

  return (
    <View style={{ padding: 20 }}>
      <Text style={{ fontSize: 20, fontWeight: 'bold', marginBottom: 16 }}>
        Cancel Order
      </Text>

      <View style={{ marginBottom: 16 }}>
        <Text style={{ fontSize: 16, marginBottom: 8 }}>Order Details:</Text>
        <Text style={{ fontSize: 14, color: '#666' }}>
          Quantity: {orderDetails.quantity}
        </Text>
        <Text style={{ fontSize: 14, color: '#666' }}>
          Total: {orderDetails.totalPrice.toFixed(2)} {orderDetails.currency}
        </Text>
      </View>

      <Text style={{ fontSize: 16, marginBottom: 8 }}>
        Reason for cancellation (optional):
      </Text>
      <TextInput
        style={{
          borderWidth: 1,
          borderColor: '#ddd',
          borderRadius: 8,
          padding: 12,
          minHeight: 100,
          textAlignVertical: 'top',
          marginBottom: 16
        }}
        multiline
        numberOfLines={4}
        placeholder="e.g., Changed my mind, Found alternative, etc."
        value={reason}
        onChangeText={setReason}
      />

      <View style={{ flexDirection: 'row', gap: 12 }}>
        <Button
          title="Go Back"
          onPress={onCancel}
          disabled={loading}
        />
        <Button
          title={loading ? 'Cancelling...' : 'Cancel Order'}
          onPress={handleCancel}
          disabled={loading}
          color="#F44336"
        />
      </View>

      {loading && <ActivityIndicator style={{ marginTop: 20 }} />}

      <View style={{ marginTop: 20, padding: 12, backgroundColor: '#FFF3CD', borderRadius: 8 }}>
        <Text style={{ fontSize: 12, color: '#856404' }}>
          ℹ️ Note: Cancelling this order will release the payment authorization. 
          You will not be charged for this order.
        </Text>
      </View>
    </View>
  );
}
```

### Vendor Order Rejection Screen

```typescript
// screens/VendorOrderRejectionScreen.tsx
import React, { useState } from 'react';
import { View, Text, TextInput, Button, Alert, ActivityIndicator, ScrollView } from 'react-native';
import { orderService } from '../services/orderService';

interface VendorOrderRejectionScreenProps {
  orderId: number;
  orderDetails: {
    quantity: number;
    totalPrice: number;
    currency: string;
    status: string;
    customerName?: string;
  };
  onRejected: () => void;
  onCancel: () => void;
}

export function VendorOrderRejectionScreen({
  orderId,
  orderDetails,
  onRejected,
  onCancel
}: VendorOrderRejectionScreenProps) {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);

  const predefinedReasons = [
    'Out of stock',
    'Offer expired',
    'Quality issue',
    'Unable to fulfill',
    'Other'
  ];

  const handleReject = async () => {
    // Check if order can be rejected
    if (orderDetails.status === 'COMPLETED') {
      Alert.alert('Cannot Reject', 'This order has already been completed and cannot be rejected.');
      return;
    }

    if (orderDetails.status === 'CANCELLED') {
      Alert.alert('Already Cancelled', 'This order has already been cancelled.');
      return;
    }

    if (!reason.trim()) {
      Alert.alert('Reason Required', 'Please provide a reason for rejecting this order.');
      return;
    }

    Alert.alert(
      'Reject Order',
      `Are you sure you want to reject order #${orderId}? The customer will be notified and payment authorization will be released.`,
      [
        { text: 'No', style: 'cancel', onPress: onCancel },
        {
          text: 'Yes, Reject',
          style: 'destructive',
          onPress: async () => {
            setLoading(true);
            try {
              const result = await orderService.rejectOrder(orderId, reason);
              
              if (result.success) {
                Alert.alert(
                  'Order Rejected',
                  'The order has been rejected successfully. The customer has been notified.',
                  [{ text: 'OK', onPress: onRejected }]
                );
              } else {
                Alert.alert('Error', result.message || 'Failed to reject order');
              }
            } catch (error) {
              Alert.alert(
                'Error',
                error instanceof Error ? error.message : 'Failed to reject order. Please try again.'
              );
            } finally {
              setLoading(false);
            }
          }
        }
      ]
    );
  };

  return (
    <ScrollView style={{ padding: 20 }}>
      <Text style={{ fontSize: 20, fontWeight: 'bold', marginBottom: 16 }}>
        Reject Order
      </Text>

      <View style={{ marginBottom: 16 }}>
        <Text style={{ fontSize: 16, marginBottom: 8 }}>Order Details:</Text>
        <Text style={{ fontSize: 14, color: '#666' }}>
          Order ID: #{orderId}
        </Text>
        {orderDetails.customerName && (
          <Text style={{ fontSize: 14, color: '#666' }}>
            Customer: {orderDetails.customerName}
          </Text>
        )}
        <Text style={{ fontSize: 14, color: '#666' }}>
          Quantity: {orderDetails.quantity}
        </Text>
        <Text style={{ fontSize: 14, color: '#666' }}>
          Total: {orderDetails.totalPrice.toFixed(2)} {orderDetails.currency}
        </Text>
      </View>

      <Text style={{ fontSize: 16, marginBottom: 8, fontWeight: 'bold' }}>
        Reason for rejection (required):
      </Text>

      <View style={{ marginBottom: 12 }}>
        {predefinedReasons.map((predefinedReason, index) => (
          <Button
            key={index}
            title={predefinedReason}
            onPress={() => {
              if (predefinedReason === 'Other') {
                setReason('');
              } else {
                setReason(predefinedReason);
              }
            }}
            color={reason === predefinedReason ? '#2196F3' : '#757575'}
          />
        ))}
      </View>

      <TextInput
        style={{
          borderWidth: 1,
          borderColor: '#ddd',
          borderRadius: 8,
          padding: 12,
          minHeight: 100,
          textAlignVertical: 'top',
          marginBottom: 16
        }}
        multiline
        numberOfLines={4}
        placeholder="Enter reason for rejection..."
        value={reason}
        onChangeText={setReason}
      />

      <View style={{ flexDirection: 'row', gap: 12, marginBottom: 16 }}>
        <Button
          title="Go Back"
          onPress={onCancel}
          disabled={loading}
        />
        <Button
          title={loading ? 'Rejecting...' : 'Reject Order'}
          onPress={handleReject}
          disabled={loading || !reason.trim()}
          color="#F44336"
        />
      </View>

      {loading && <ActivityIndicator style={{ marginTop: 20 }} />}

      <View style={{ marginTop: 20, padding: 12, backgroundColor: '#FFF3CD', borderRadius: 8 }}>
        <Text style={{ fontSize: 12, color: '#856404' }}>
          ⚠️ Warning: Rejecting this order will notify the customer and release their payment authorization. 
          The offer quantity will be restored automatically.
        </Text>
      </View>
    </ScrollView>
  );
}
```

### Integration with Order Details Screen

```typescript
// screens/OrderDetailsScreen.tsx
import React from 'react';
import { View, Text, Button, Alert } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { orderService, Order } from '../services/orderService';
import { useAuth } from '../context/AuthContext';

export function OrderDetailsScreen({ orderId }: { orderId: number }) {
  const navigation = useNavigation();
  const { user, role } = useAuth();
  const [order, setOrder] = useState<Order | null>(null);

  useEffect(() => {
    loadOrder();
  }, [orderId]);

  const loadOrder = async () => {
    try {
      const orderData = await orderService.getOrder(orderId);
      setOrder(orderData);
    } catch (error) {
      Alert.alert('Error', 'Failed to load order details');
    }
  };

  const handleCancel = () => {
    if (role === 'CUSTOMER') {
      navigation.navigate('CustomerOrderCancellation', { orderId, order });
    }
  };

  const handleReject = () => {
    if (role === 'VENDOR') {
      navigation.navigate('VendorOrderRejection', { orderId, order });
    }
  };

  const canCancel = order && 
    order.status !== 'COMPLETED' && 
    order.status !== 'CANCELLED' &&
    role === 'CUSTOMER';

  const canReject = order && 
    order.status !== 'COMPLETED' && 
    order.status !== 'CANCELLED' &&
    role === 'VENDOR' &&
    (order.status === 'PENDING' || order.status === 'CONFIRMED');

  return (
    <View style={{ padding: 20 }}>
      {/* Order details display */}
      
      {canCancel && (
        <Button
          title="Cancel Order"
          onPress={handleCancel}
          color="#F44336"
        />
      )}

      {canReject && (
        <Button
          title="Reject Order"
          onPress={handleReject}
          color="#F44336"
        />
      )}
    </View>
  );
}
```

---

## Error Handling

### Common Errors and Solutions

#### 1. "Order is already cancelled"
- **Cause**: Order was already cancelled
- **Solution**: Check order status before showing cancel/reject button
- **User Action**: Show message: "This order has already been cancelled"

#### 2. "Cannot cancel order - it is already completed"
- **Cause**: Order is in COMPLETED status
- **Solution**: Hide cancel/reject buttons for completed orders
- **User Action**: Show message: "This order has already been completed and cannot be cancelled"

#### 3. "Order not found"
- **Cause**: Order doesn't exist or user doesn't have access
- **Solution**: Verify order ID and user permissions
- **User Action**: Show error message, refresh order list

#### 4. 401 Unauthorized
- **Cause**: JWT token expired or invalid
- **Solution**: Clear token and redirect to login
- **User Action**: User must log in again

#### 5. 403 Forbidden
- **Cause**: User doesn't have permission (e.g., customer trying to reject, vendor trying to cancel someone else's order)
- **Solution**: Verify order ownership
- **User Action**: Show error: "You don't have permission to perform this action"

### Error Handling Example

```typescript
const handleCancelOrder = async (orderId: number, reason?: string) => {
  try {
    // Validate order can be cancelled
    const order = await orderService.getOrder(orderId);
    
    if (order.status === 'COMPLETED') {
      Alert.alert('Cannot Cancel', 'This order has already been completed.');
      return;
    }
    
    if (order.status === 'CANCELLED') {
      Alert.alert('Already Cancelled', 'This order has already been cancelled.');
      return;
    }

    const result = await orderService.cancelOrder(orderId, reason);
    
    if (result.success) {
      Alert.alert('Success', 'Order cancelled successfully');
      refreshOrders();
    } else {
      Alert.alert('Error', result.message || 'Failed to cancel order');
    }
  } catch (error) {
    if (error.response?.status === 401) {
      // Token expired
      await logout();
      navigateToLogin();
    } else if (error.response?.status === 403) {
      Alert.alert('Permission Denied', 'You don\'t have permission to cancel this order');
    } else if (error.response?.status === 400) {
      Alert.alert('Error', error.response.data.message || 'Invalid request');
    } else {
      Alert.alert('Error', 'Failed to cancel order. Please try again.');
    }
  }
};
```

---

## UI/UX Guidelines

### Customer Cancellation

1. **Cancel Button Placement**:
   - Show on order details screen
   - Show in order list (for cancellable orders)
   - Hide for COMPLETED or CANCELLED orders

2. **Cancellation Flow**:
   - Show confirmation dialog
   - Optional reason field
   - Clear messaging about payment release
   - Success message after cancellation

3. **Visual Indicators**:
   - Use red/destructive color for cancel button
   - Show warning icon for important actions
   - Display order details before cancellation

### Vendor Rejection

1. **Reject Button Placement**:
   - Show on vendor order management screen
   - Show in order details (for rejectable orders)
   - Hide for COMPLETED or CANCELLED orders

2. **Rejection Flow**:
   - Require reason for rejection
   - Provide predefined reasons (quick selection)
   - Show confirmation dialog
   - Clear messaging about customer notification
   - Success message after rejection

3. **Visual Indicators**:
   - Use red/destructive color for reject button
   - Show warning icon
   - Display order and customer details

### Push Notification Handling

1. **Notification Display**:
   - Show notification when app is in foreground
   - Handle deep linking to order details
   - Update order list when notification received

2. **Notification Actions**:
   - Tap notification → Navigate to order details
   - Show order status update immediately
   - Refresh order list automatically

---

## Testing Checklist

### Customer Cancellation
- [ ] Customer can cancel their own orders
- [ ] Customer cannot cancel completed orders
- [ ] Customer cannot cancel already cancelled orders
- [ ] Cancellation confirmation dialog works
- [ ] Optional reason field works
- [ ] Success message displays correctly
- [ ] Order list updates after cancellation
- [ ] Payment authorization is released
- [ ] Offer quantity is restored
- [ ] Vendor receives push notification
- [ ] Error handling works for all error cases

### Vendor Rejection
- [ ] Vendor can reject their own orders
- [ ] Vendor cannot reject completed orders
- [ ] Vendor cannot reject already cancelled orders
- [ ] Reason is required for rejection
- [ ] Predefined reasons work
- [ ] Custom reason input works
- [ ] Rejection confirmation dialog works
- [ ] Success message displays correctly
- [ ] Order list updates after rejection
- [ ] Payment authorization is released
- [ ] Offer quantity is restored
- [ ] Customer receives push notification
- [ ] Error handling works for all error cases

### Push Notifications
- [ ] Vendor receives notification when customer cancels
- [ ] Customer receives notification when vendor rejects
- [ ] Notification data is correct
- [ ] Notification deep linking works
- [ ] Order list refreshes on notification
- [ ] Notification badge updates correctly

### Edge Cases
- [ ] Cancelling order with PaymentIntent releases hold
- [ ] Cancelling order without PaymentIntent works
- [ ] Multiple rapid cancellation attempts are handled
- [ ] Network errors during cancellation are handled
- [ ] Token expiration during cancellation redirects to login

---

## Integration with Existing Features

### Order Status Management

Cancellation/rejection integrates with order status management:
- Order status changes to `CANCELLED`
- Status update is reflected in order list
- Status cannot be changed after cancellation

**Refer to**: `MOBILE_APP_ORDER_STATUS_MANAGEMENT_PROMPT.md` for status management details

### Payment Flow

Cancellation automatically handles payment:
- PaymentIntent is cancelled (if exists)
- Payment authorization is released
- Customer is not charged

**Refer to**: `MOBILE_APP_TOO_GOOD_TO_GO_PAYMENT_FLOW.md` for payment flow details

### Notification System

Push notifications are sent automatically:
- Uses Firebase Cloud Messaging (FCM)
- Requires FCM token registration
- Handles notification data for deep linking

**Refer to**: Notification service documentation for FCM token registration

---

## State Management Recommendations

```typescript
// Example state structure
interface OrderCancellationState {
  cancellingOrderId: number | null;
  rejectingOrderId: number | null;
  cancellationReason: string;
  rejectionReason: string;
  loading: boolean;
  error: string | null;
}
```

### React Context Example

```typescript
// context/OrderCancellationContext.tsx
import React, { createContext, useContext, useState, useCallback } from 'react';
import { orderService } from '../services/orderService';

interface OrderCancellationContextType {
  cancelOrder: (orderId: number, reason?: string) => Promise<boolean>;
  rejectOrder: (orderId: number, reason: string) => Promise<boolean>;
  loading: boolean;
  error: string | null;
}

const OrderCancellationContext = createContext<OrderCancellationContextType | undefined>(undefined);

export function OrderCancellationProvider({ children }: { children: React.ReactNode }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cancelOrder = useCallback(async (orderId: number, reason?: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await orderService.cancelOrder(orderId, reason);
      if (result.success) {
        return true;
      } else {
        setError(result.message);
        return false;
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel order');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  const rejectOrder = useCallback(async (orderId: number, reason: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await orderService.rejectOrder(orderId, reason);
      if (result.success) {
        return true;
      } else {
        setError(result.message);
        return false;
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject order');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <OrderCancellationContext.Provider
      value={{
        cancelOrder,
        rejectOrder,
        loading,
        error
      }}
    >
      {children}
    </OrderCancellationContext.Provider>
  );
}

export function useOrderCancellation() {
  const context = useContext(OrderCancellationContext);
  if (!context) {
    throw new Error('useOrderCancellation must be used within OrderCancellationProvider');
  }
  return context;
}
```

---

## Security Best Practices

1. **Authorization**: Verify user can only cancel/reject their own orders
2. **Token Storage**: Store JWT tokens securely (Keychain/SecureStorage)
3. **Input Validation**: Validate reason length and content
4. **HTTPS Only**: Always use HTTPS in production
5. **Error Messages**: Don't expose sensitive backend details to users
6. **Rate Limiting**: Implement client-side rate limiting for cancellation requests

---

## Performance Optimization

1. **Optimistic Updates**: Show cancellation immediately, rollback on error
2. **Debounce**: Debounce rapid cancellation attempts
3. **Cache Invalidation**: Invalidate order cache after cancellation
4. **Background Refresh**: Refresh order list in background after cancellation

---

## Summary

### Key Points

1. ✅ **Customer Cancellation**: `PUT /orders/{id}/cancel` - Customer can cancel their orders
2. ✅ **Vendor Rejection**: `PUT /orders/{id}/reject` - Vendor can reject orders
3. ✅ **Automatic Actions**: Offer quantity restored, payment released automatically
4. ✅ **Push Notifications**: Automatic notifications sent to the other party
5. ✅ **Status Validation**: Prevents cancelling completed or already cancelled orders
6. ✅ **Optional Reason**: Reason field is optional for customers, required for vendors
7. ✅ **Error Handling**: Comprehensive error handling for all scenarios

### Quick Reference

**Customer Cancel Order**:
```
PUT /orders/{orderId}/cancel
Body (optional): { "reason": "Changed my mind" }
```

**Vendor Reject Order**:
```
PUT /orders/{orderId}/reject
Body (optional): { "reason": "Out of stock" }
```

**What Happens on Cancellation/Rejection**:
1. Order status → `CANCELLED`
2. Offer quantity increased by cancelled quantity
3. Payment authorization released (if exists)
4. Push notification sent to other party

### Related Documents

- `MOBILE_APP_ORDER_STATUS_MANAGEMENT_PROMPT.md` - Order status management
- `MOBILE_APP_ORDER_PLACEMENT_INTEGRATION_PROMPT.md` - Order placement
- `MOBILE_APP_TOO_GOOD_TO_GO_PAYMENT_FLOW.md` - Payment flow
- `FRONTEND_API_GATEWAY_INTEGRATION_GUIDE.md` - API Gateway setup

---

**Last Updated**: Based on API version as of implementation date
**API Base URL**: Configure based on environment (dev/staging/production)





