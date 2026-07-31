# Mobile App AI Agent Prompt: Order Status Management Integration

## Overview
You are an AI agent responsible for implementing order status management functionality in the StillFresh mobile application. This guide explains how to integrate vendor order confirmation/refusal and order status tracking features, allowing vendors to manage orders and customers to track their order progress.

**Key Concept**: Orders have a status lifecycle that can be updated by vendors. The mobile app must provide interfaces for both vendors (to manage orders) and customers (to track orders).

## Base API Configuration
- **Base URL**: `http://localhost:8080` (development) or your production API Gateway URL
- **Authentication**: JWT Bearer token required for all order endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

**⚠️ IMPORTANT**: All requests must go through the API Gateway, not directly to individual services.

## Authentication

Before accessing order status endpoints, the user must be authenticated. Include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

If you receive a `401 Unauthorized` response, the token has expired. Redirect the user to the login screen.

---

## Order Status Endpoint

### Update Order Status

**Endpoint**: `PUT /orders/{orderId}/status`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "status": "CONFIRMED"
}
```

**Path Parameters**:
- `orderId` (number, required): The ID of the order to update

**Request Fields**:
- `status` (string, required): The new status for the order (see Valid Status Values below)

**Success Response (200 OK)**:
```json
{
  "id": 7,
  "offerId": 123,
  "userId": 456,
  "quantity": 2,
  "unitPrice": 1500.00,
  "totalPrice": 3000.00,
  "vendorId": 789,
  "currency": "RSD",
  "status": "CONFIRMED",
  "paymentIntentId": "pi_xxxxx",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

**Error Responses**:
- **400 Bad Request**: 
  - `{"error": "Invalid status value"}`
  - `{"error": "Order not found with ID: {orderId}"}`
- **401 Unauthorized**: Token expired or invalid - redirect to login
- **404 Not Found**: Order not found or user doesn't have access
- **500 Internal Server Error**: Server error - retry or show error message

**Implementation Notes**:
- Only vendors can update status of their own orders
- Customers can view order status but cannot update it
- Status transitions should follow the order lifecycle (see Status Transitions section)

---

## Valid Order Status Values

| Status | Description | Who Can Set | Next Valid Statuses |
|--------|-------------|-------------|---------------------|
| `PENDING` | Order placed, awaiting vendor confirmation | System (default) | `CONFIRMED`, `CANCELLED` |
| `CONFIRMED` | Vendor confirmed the order | Vendor | `PROCESSING`, `CANCELLED` |
| `PROCESSING` | Vendor is preparing the order | Vendor | `READY`, `CANCELLED` |
| `READY` | Order is ready for pickup/delivery | Vendor | `COMPLETED`, `CANCELLED` |
| `COMPLETED` | Order has been completed | Vendor | (final state) |
| `CANCELLED` | Order was cancelled | Vendor, Customer | (final state) |

### Status Descriptions

#### PENDING
- **Initial State**: All new orders start with this status
- **Meaning**: Order has been placed and payment processed, but vendor hasn't confirmed yet
- **User Action**: Vendor should review and confirm or refuse
- **Customer View**: "Order placed, waiting for vendor confirmation"

#### CONFIRMED
- **Set By**: Vendor confirms they can fulfill the order
- **Meaning**: Vendor has accepted the order and will prepare it
- **User Action**: Vendor begins preparing the order
- **Customer View**: "Order confirmed! Vendor is preparing your order"

#### PROCESSING
- **Set By**: Vendor when they start preparing the order
- **Meaning**: Order is being prepared/packaged
- **User Action**: Customer waits, vendor continues preparation
- **Customer View**: "Your order is being prepared"

#### READY
- **Set By**: Vendor when order is ready for pickup/delivery
- **Meaning**: Order is complete and ready for customer
- **User Action**: Customer can pick up or wait for delivery
- **Customer View**: "Your order is ready! You can pick it up now"

#### COMPLETED
- **Set By**: Vendor when order has been picked up/delivered
- **Meaning**: Order fulfillment is complete
- **User Action**: Customer can rate/review the order
- **Customer View**: "Order completed! How was your experience?"

#### CANCELLED
- **Set By**: Vendor or Customer (depending on business rules)
- **Meaning**: Order was cancelled and will not be fulfilled
- **User Action**: Refund may be processed automatically
- **Customer View**: "Order cancelled. Refund processing..."

---

## Status Transitions

### Valid Status Flow

```
PENDING
  ├──> CONFIRMED (vendor confirms)
  └──> CANCELLED (vendor refuses or customer cancels)

CONFIRMED
  ├──> PROCESSING (vendor starts preparation)
  └──> CANCELLED (vendor or customer cancels)

PROCESSING
  ├──> READY (vendor completes preparation)
  └──> CANCELLED (vendor or customer cancels)

READY
  ├──> COMPLETED (customer picks up/delivered)
  └──> CANCELLED (rare, but possible)

COMPLETED (final state - no transitions)
CANCELLED (final state - no transitions)
```

### Invalid Transitions

The following transitions are **NOT allowed** and should be prevented in the UI:
- `PENDING` → `PROCESSING` (must confirm first)
- `PENDING` → `READY` (must confirm and process first)
- `PENDING` → `COMPLETED` (must go through all stages)
- `CONFIRMED` → `READY` (must process first)
- `CONFIRMED` → `COMPLETED` (must process and mark ready first)
- `PROCESSING` → `COMPLETED` (must mark ready first)
- Any status → `PENDING` (cannot go backwards)
- `COMPLETED` → any status (final state)
- `CANCELLED` → any status (final state)

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

export interface UpdateOrderStatusRequest {
  status: Order['status'];
}

export const orderService = {
  // ... existing methods ...

  /**
   * Update order status (vendor only)
   */
  async updateOrderStatus(
    orderId: number, 
    status: Order['status']
  ): Promise<Order> {
    return apiClient.put<Order>(`/orders/${orderId}/status`, { status });
  },

  /**
   * Get orders for vendor (filtered by vendorId from token)
   */
  async getVendorOrders(options?: {
    status?: Order['status'];
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
  }
};
```

### Vendor Order Management Screen

```typescript
// screens/VendorOrderManagementScreen.tsx
import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { orderService, Order } from '../services/orderService';

export function VendorOrderManagementScreen() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingOrderId, setUpdatingOrderId] = useState<number | null>(null);

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders = async () => {
    try {
      setLoading(true);
      const orderList = await orderService.getVendorOrders({ limit: 50 });
      setOrders(orderList);
    } catch (error) {
      Alert.alert('Error', 'Failed to load orders');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const updateOrderStatus = async (orderId: number, newStatus: Order['status']) => {
    try {
      setUpdatingOrderId(orderId);
      const updatedOrder = await orderService.updateOrderStatus(orderId, newStatus);
      
      // Update local state
      setOrders(orders.map(order => 
        order.id === orderId ? updatedOrder : order
      ));
      
      Alert.alert('Success', `Order status updated to ${newStatus}`);
    } catch (error) {
      Alert.alert('Error', 'Failed to update order status');
      console.error(error);
    } finally {
      setUpdatingOrderId(null);
    }
  };

  const getNextValidStatuses = (currentStatus: Order['status']): Order['status'][] => {
    switch (currentStatus) {
      case 'PENDING':
        return ['CONFIRMED', 'CANCELLED'];
      case 'CONFIRMED':
        return ['PROCESSING', 'CANCELLED'];
      case 'PROCESSING':
        return ['READY', 'CANCELLED'];
      case 'READY':
        return ['COMPLETED', 'CANCELLED'];
      case 'COMPLETED':
      case 'CANCELLED':
        return []; // Final states
      default:
        return [];
    }
  };

  const getStatusColor = (status: Order['status']) => {
    switch (status) {
      case 'PENDING': return '#FFA500'; // Orange
      case 'CONFIRMED': return '#4CAF50'; // Green
      case 'PROCESSING': return '#2196F3'; // Blue
      case 'READY': return '#9C27B0'; // Purple
      case 'COMPLETED': return '#4CAF50'; // Green
      case 'CANCELLED': return '#F44336'; // Red
      default: return '#757575'; // Gray
    }
  };

  const getStatusLabel = (status: Order['status']) => {
    switch (status) {
      case 'PENDING': return 'Pending Confirmation';
      case 'CONFIRMED': return 'Confirmed';
      case 'PROCESSING': return 'Processing';
      case 'READY': return 'Ready for Pickup';
      case 'COMPLETED': return 'Completed';
      case 'CANCELLED': return 'Cancelled';
      default: return status;
    }
  };

  const handleStatusChange = (order: Order, newStatus: Order['status']) => {
    if (newStatus === 'CANCELLED') {
      Alert.alert(
        'Cancel Order',
        'Are you sure you want to cancel this order?',
        [
          { text: 'No', style: 'cancel' },
          { 
            text: 'Yes', 
            style: 'destructive',
            onPress: () => updateOrderStatus(order.id, newStatus)
          }
        ]
      );
    } else {
      updateOrderStatus(order.id, newStatus);
    }
  };

  if (loading) {
    return <ActivityIndicator style={{ flex: 1, justifyContent: 'center' }} />;
  }

  return (
    <View style={{ flex: 1, padding: 16 }}>
      <Text style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 16 }}>
        Manage Orders
      </Text>

      <FlatList
        data={orders}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => {
          const nextStatuses = getNextValidStatuses(item.status);
          const isUpdating = updatingOrderId === item.id;

          return (
            <View style={{
              padding: 16,
              marginBottom: 12,
              backgroundColor: '#fff',
              borderRadius: 8,
              borderLeftWidth: 4,
              borderLeftColor: getStatusColor(item.status)
            }}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 }}>
                <Text style={{ fontSize: 18, fontWeight: 'bold' }}>
                  Order #{item.id}
                </Text>
                <View style={{
                  paddingHorizontal: 8,
                  paddingVertical: 4,
                  backgroundColor: getStatusColor(item.status) + '20',
                  borderRadius: 4
                }}>
                  <Text style={{
                    color: getStatusColor(item.status),
                    fontWeight: 'bold',
                    fontSize: 12
                  }}>
                    {getStatusLabel(item.status)}
                  </Text>
                </View>
              </View>

              <Text style={{ fontSize: 14, color: '#666', marginBottom: 4 }}>
                Quantity: {item.quantity}
              </Text>
              <Text style={{ fontSize: 16, fontWeight: 'bold', marginBottom: 8 }}>
                Total: {item.totalPrice.toFixed(2)} {item.currency}
              </Text>
              <Text style={{ fontSize: 12, color: '#999' }}>
                Created: {new Date(item.createdAt).toLocaleString()}
              </Text>

              {nextStatuses.length > 0 && (
                <View style={{ marginTop: 12, flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
                  {nextStatuses.map((status) => (
                    <TouchableOpacity
                      key={status}
                      disabled={isUpdating}
                      onPress={() => handleStatusChange(item, status)}
                      style={{
                        paddingHorizontal: 12,
                        paddingVertical: 6,
                        backgroundColor: isUpdating ? '#ccc' : getStatusColor(status),
                        borderRadius: 4
                      }}
                    >
                      <Text style={{ color: '#fff', fontWeight: 'bold', fontSize: 12 }}>
                        {isUpdating ? 'Updating...' : getStatusLabel(status)}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>
              )}

              {(item.status === 'COMPLETED' || item.status === 'CANCELLED') && (
                <Text style={{ 
                  marginTop: 8, 
                  fontSize: 12, 
                  color: '#999', 
                  fontStyle: 'italic' 
                }}>
                  This order is in a final state and cannot be changed.
                </Text>
              )}
            </View>
          );
        }}
        ListEmptyComponent={
          <View style={{ padding: 20, alignItems: 'center' }}>
            <Text style={{ fontSize: 16, color: '#666' }}>
              No orders found
            </Text>
          </View>
        }
        refreshing={loading}
        onRefresh={loadOrders}
      />
    </View>
  );
}
```

### Customer Order Status Tracking Screen

```typescript
// screens/CustomerOrderStatusScreen.tsx
import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, RefreshControl, ActivityIndicator } from 'react-native';
import { orderService, Order } from '../services/orderService';

export function CustomerOrderStatusScreen() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadOrders();
    
    // Poll for updates every 10 seconds
    const interval = setInterval(loadOrders, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadOrders = async () => {
    try {
      const orderList = await orderService.getOrders({ limit: 50 });
      setOrders(orderList);
    } catch (error) {
      console.error('Failed to load orders:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: Order['status']) => {
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

  const getStatusMessage = (status: Order['status']) => {
    switch (status) {
      case 'PENDING':
        return 'Order placed! Waiting for vendor confirmation...';
      case 'CONFIRMED':
        return 'Order confirmed! Vendor is preparing your order.';
      case 'PROCESSING':
        return 'Your order is being prepared.';
      case 'READY':
        return 'Your order is ready! You can pick it up now.';
      case 'COMPLETED':
        return 'Order completed! How was your experience?';
      case 'CANCELLED':
        return 'Order cancelled. Refund processing...';
      default:
        return status;
    }
  };

  const getStatusIcon = (status: Order['status']) => {
    switch (status) {
      case 'PENDING': return '⏳';
      case 'CONFIRMED': return '✅';
      case 'PROCESSING': return '👨‍🍳';
      case 'READY': return '📦';
      case 'COMPLETED': return '🎉';
      case 'CANCELLED': return '❌';
      default: return '•';
    }
  };

  if (loading && orders.length === 0) {
    return <ActivityIndicator style={{ flex: 1, justifyContent: 'center' }} />;
  }

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={orders}
        keyExtractor={(item) => item.id.toString()}
        refreshControl={
          <RefreshControl refreshing={loading} onRefresh={loadOrders} />
        }
        renderItem={({ item }) => (
          <View style={{
            padding: 16,
            margin: 12,
            backgroundColor: '#fff',
            borderRadius: 8,
            borderLeftWidth: 4,
            borderLeftColor: getStatusColor(item.status)
          }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
              <Text style={{ fontSize: 24, marginRight: 8 }}>
                {getStatusIcon(item.status)}
              </Text>
              <View style={{ flex: 1 }}>
                <Text style={{ fontSize: 18, fontWeight: 'bold' }}>
                  Order #{item.id}
                </Text>
                <Text style={{
                  fontSize: 14,
                  color: getStatusColor(item.status),
                  fontWeight: 'bold',
                  marginTop: 4
                }}>
                  {getStatusMessage(item.status)}
                </Text>
              </View>
            </View>

            <View style={{
              height: 4,
              backgroundColor: '#e0e0e0',
              borderRadius: 2,
              marginVertical: 12,
              overflow: 'hidden'
            }}>
              <View style={{
                height: '100%',
                width: getProgressPercentage(item.status) + '%',
                backgroundColor: getStatusColor(item.status),
                borderRadius: 2
              }} />
            </View>

            <Text style={{ fontSize: 14, color: '#666', marginBottom: 4 }}>
              Quantity: {item.quantity}
            </Text>
            <Text style={{ fontSize: 16, fontWeight: 'bold', marginBottom: 4 }}>
              Total: {item.totalPrice.toFixed(2)} {item.currency}
            </Text>
            <Text style={{ fontSize: 12, color: '#999' }}>
              Placed: {new Date(item.createdAt).toLocaleString()}
            </Text>
            {item.updatedAt && (
              <Text style={{ fontSize: 12, color: '#999' }}>
                Updated: {new Date(item.updatedAt).toLocaleString()}
              </Text>
            )}
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

function getProgressPercentage(status: Order['status']): number {
  switch (status) {
    case 'PENDING': return 10;
    case 'CONFIRMED': return 30;
    case 'PROCESSING': return 60;
    case 'READY': return 90;
    case 'COMPLETED': return 100;
    case 'CANCELLED': return 0;
    default: return 0;
  }
}
```

---

## Error Handling

### Common Errors and Solutions

#### 1. "Invalid status value"
- **Cause**: Status value doesn't match valid statuses
- **Solution**: Validate status before sending request
- **User Action**: Show error message, don't allow invalid transitions

#### 2. "Order not found with ID: {orderId}"
- **Cause**: Order doesn't exist or user doesn't have access
- **Solution**: Verify order ID and user permissions
- **User Action**: Show error message, refresh order list

#### 3. 401 Unauthorized
- **Cause**: JWT token expired or invalid
- **Solution**: Clear token and redirect to login
- **User Action**: User must log in again

#### 4. Network Error
- **Cause**: Network connectivity issue
- **Solution**: Retry with exponential backoff
- **User Action**: Check internet connection and try again

### Error Handling Example

```typescript
const updateOrderStatus = async (orderId: number, status: Order['status']) => {
  try {
    // Validate status transition
    const currentOrder = orders.find(o => o.id === orderId);
    if (!currentOrder) {
      throw new Error('Order not found');
    }

    const validNextStatuses = getNextValidStatuses(currentOrder.status);
    if (!validNextStatuses.includes(status)) {
      Alert.alert(
        'Invalid Status',
        `Cannot change status from ${currentOrder.status} to ${status}`
      );
      return;
    }

    const updatedOrder = await orderService.updateOrderStatus(orderId, status);
    // Update local state...
  } catch (error) {
    if (error.response?.status === 401) {
      // Token expired
      await logout();
      navigateToLogin();
    } else if (error.response?.status === 400) {
      Alert.alert('Error', error.response.data.error || 'Invalid request');
    } else {
      Alert.alert('Error', 'Failed to update order status. Please try again.');
    }
  }
};
```

---

## UI/UX Guidelines

### Vendor Order Management

1. **Order List**:
   - Group orders by status (tabs or filters)
   - Show most recent orders first
   - Display order details clearly
   - Use color coding for status

2. **Status Update Buttons**:
   - Show only valid next statuses as buttons
   - Disable buttons during update
   - Show loading state on active button
   - Confirm destructive actions (CANCELLED)

3. **Status Indicators**:
   - Use icons and colors for each status
   - Show status text clearly
   - Display timestamps

4. **Pull to Refresh**:
   - Allow vendors to manually refresh
   - Show loading indicator during refresh

### Customer Order Tracking

1. **Order Status Display**:
   - Show progress bar indicating order completion
   - Use icons and colors for visual feedback
   - Display clear status messages

2. **Real-time Updates**:
   - Poll for status updates every 10-30 seconds
   - Show notification when status changes
   - Update UI immediately on status change

3. **Order Details**:
   - Show full order information
   - Display estimated time if available
   - Link to vendor information

---

## Status Transition Validation

### Client-Side Validation

Always validate status transitions on the client side before making API calls:

```typescript
const isValidTransition = (
  currentStatus: Order['status'],
  newStatus: Order['status']
): boolean => {
  const validTransitions: Record<Order['status'], Order['status'][]> = {
    PENDING: ['CONFIRMED', 'CANCELLED'],
    CONFIRMED: ['PROCESSING', 'CANCELLED'],
    PROCESSING: ['READY', 'CANCELLED'],
    READY: ['COMPLETED', 'CANCELLED'],
    COMPLETED: [], // Final state
    CANCELLED: []  // Final state
  };

  return validTransitions[currentStatus]?.includes(newStatus) ?? false;
};
```

### UI State Management

```typescript
// Only show buttons for valid next statuses
const nextStatuses = getNextValidStatuses(order.status);

// Disable buttons for final states
const isFinalState = order.status === 'COMPLETED' || order.status === 'CANCELLED';
```

---

## Testing Checklist

### Vendor Order Management
- [ ] Vendor can view their orders
- [ ] Status buttons show only valid next statuses
- [ ] Status update succeeds
- [ ] Order list updates after status change
- [ ] Cancellation shows confirmation dialog
- [ ] Final states don't show status buttons
- [ ] Error handling works for all error cases
- [ ] Token expiration is handled correctly

### Customer Order Tracking
- [ ] Customer can view their orders
- [ ] Status is displayed correctly with icons/colors
- [ ] Progress bar shows correct percentage
- [ ] Status updates in real-time (polling)
- [ ] Pull to refresh works
- [ ] Order details show correctly
- [ ] Timestamps display correctly

### Status Transitions
- [ ] PENDING → CONFIRMED works
- [ ] CONFIRMED → PROCESSING works
- [ ] PROCESSING → READY works
- [ ] READY → COMPLETED works
- [ ] Any status → CANCELLED works
- [ ] Invalid transitions are prevented
- [ ] Final states cannot be changed

### Error Scenarios
- [ ] Invalid status value shows error
- [ ] Order not found shows error
- [ ] Network errors are handled gracefully
- [ ] 401 Unauthorized redirects to login
- [ ] Validation errors show appropriate messages

---

## Integration with Existing Features

### Order Placement Flow

After a customer places an order:
1. Order is created with status `PENDING`
2. Vendor receives notification
3. Vendor can confirm or refuse via status update
4. Customer sees status update in real-time

### Payment Integration

- Orders with status `PENDING` or `CONFIRMED` may have payment holds
- Cancelling an order should trigger payment cancellation
- Completed orders finalize payment

**Refer to**: `MOBILE_APP_ORDER_PLACEMENT_INTEGRATION_PROMPT.md` for order placement details

### Notification Integration

- Send push notifications when order status changes
- Notify customer when vendor confirms order
- Notify customer when order is ready

**Refer to**: Notification service documentation for push notification setup

---

## State Management Recommendations

```typescript
// Example state structure
interface OrderManagementState {
  orders: Order[];
  selectedOrder: Order | null;
  filters: {
    status?: Order['status'];
    limit: number;
    offset: number;
  };
  loading: boolean;
  updatingOrderId: number | null;
  error: string | null;
}
```

### React Context Example

```typescript
// context/OrderManagementContext.tsx
import React, { createContext, useContext, useState, useCallback } from 'react';
import { orderService, Order } from '../services/orderService';

interface OrderManagementContextType {
  orders: Order[];
  loadOrders: () => Promise<void>;
  updateOrderStatus: (orderId: number, status: Order['status']) => Promise<void>;
  loading: boolean;
  error: string | null;
}

const OrderManagementContext = createContext<OrderManagementContextType | undefined>(undefined);

export function OrderManagementProvider({ children }: { children: React.ReactNode }) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const orderList = await orderService.getVendorOrders();
      setOrders(orderList);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, []);

  const updateOrderStatus = useCallback(async (orderId: number, status: Order['status']) => {
    try {
      const updatedOrder = await orderService.updateOrderStatus(orderId, status);
      setOrders(orders.map(order => 
        order.id === orderId ? updatedOrder : order
      ));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update order status');
      throw err;
    }
  }, [orders]);

  return (
    <OrderManagementContext.Provider
      value={{
        orders,
        loadOrders,
        updateOrderStatus,
        loading,
        error
      }}
    >
      {children}
    </OrderManagementContext.Provider>
  );
}

export function useOrderManagement() {
  const context = useContext(OrderManagementContext);
  if (!context) {
    throw new Error('useOrderManagement must be used within OrderManagementProvider');
  }
  return context;
}
```

---

## Security Best Practices

1. **Authorization**: Verify vendor can only update their own orders
2. **Token Storage**: Store JWT tokens securely (Keychain/SecureStorage)
3. **Input Validation**: Validate status values on client side
4. **HTTPS Only**: Always use HTTPS in production
5. **Error Messages**: Don't expose sensitive backend details to users
6. **Status Validation**: Always validate status transitions before API calls

---

## Performance Optimization

1. **Debounce Status Updates**: Debounce rapid status changes
2. **Optimistic Updates**: Show status change immediately, rollback on error
3. **Lazy Load Orders**: Implement pagination for order history
4. **Cache Order Data**: Cache recent orders to reduce API calls
5. **Polling Strategy**: Use exponential backoff for status polling
6. **WebSocket Alternative**: Consider WebSocket for real-time updates instead of polling

---

## Summary

### Key Points

1. ✅ **Use API Gateway** (`http://localhost:8080` or production URL)
2. ✅ **Include JWT token** in Authorization header
3. ✅ **Validate status transitions** before making API calls
4. ✅ **Show only valid next statuses** in UI
5. ✅ **Handle errors gracefully** - especially 401 (token expired)
6. ✅ **Update UI optimistically** for better UX
7. ✅ **Poll for status updates** for real-time customer experience
8. ✅ **Confirm destructive actions** (cancellation)

### Quick Reference

**Update Order Status**:
```
PUT /orders/{orderId}/status
Body: { "status": "CONFIRMED" }
```

**Valid Status Values**:
- `PENDING` - Initial state
- `CONFIRMED` - Vendor confirmed
- `PROCESSING` - Being prepared
- `READY` - Ready for pickup
- `COMPLETED` - Order completed
- `CANCELLED` - Order cancelled

**Status Flow**:
```
PENDING → CONFIRMED → PROCESSING → READY → COMPLETED
  ↓           ↓            ↓          ↓
CANCELLED  CANCELLED   CANCELLED  CANCELLED
```

### Related Documents

- `MOBILE_APP_ORDER_PLACEMENT_INTEGRATION_PROMPT.md` - Order placement
- `FRONTEND_API_GATEWAY_INTEGRATION_GUIDE.md` - API Gateway setup
- `ANDROID_GOOGLE_SIGNIN_IMPLEMENTATION.md` - Authentication

---

**Last Updated**: Based on API version as of implementation date
**API Base URL**: Configure based on environment (dev/staging/production)

