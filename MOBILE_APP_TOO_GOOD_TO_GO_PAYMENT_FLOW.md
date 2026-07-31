# Mobile App AI Agent Prompt: Too Good To Go Style Payment Flow

## Overview

You are an AI agent responsible for implementing the Too Good To Go-style payment flow in the StillFresh Android application. This payment model places a **hold** (authorization) on the customer's card when an order is placed, but only **charges** the customer when they pick up the order. If the order is cancelled, the hold is released without charging the customer.

**Key Concept**: Payment authorization ≠ Payment charge
- **Authorization** (Order Placement): Hold is placed on card, funds are reserved but not charged
- **Capture** (Pickup): Customer is actually charged, funds are transferred
- **Cancel** (Order Cancellation): Hold is released, no charge occurs

This provides better customer protection and reduces disputes, similar to Too Good To Go's trusted payment model.

## Base API Configuration

- **Base URL**: `http://localhost:8080` (development) or your production API Gateway URL
- **Authentication**: JWT Bearer token required for all payment endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

**⚠️ IMPORTANT**: All requests must go through the API Gateway, not directly to individual services.

## Authentication

Before accessing payment endpoints, the user must be authenticated. Include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

If you receive a `401 Unauthorized` response, the token has expired. Redirect the user to the login screen.

---

## Payment Flow Overview

The payment flow consists of three main stages:

```
1. Order Placement
   ↓
   PaymentIntent created with MANUAL capture
   Status: "requires_capture" (hold placed on card)
   ↓
2. Order Pickup
   ↓
   PaymentIntent captured
   Status: "succeeded" (customer charged)
   ↓
3. Order Cancellation (if needed)
   ↓
   PaymentIntent cancelled
   Status: "canceled" (hold released)
```

---

## Stage 1: Order Placement (Payment Authorization)

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

**Success Response (200 OK)**:
```json
{
  "message": "Order request submitted successfully."
}
```

**What Happens Behind the Scenes**:
1. Backend validates the offer (availability, expiration, active status)
2. PaymentIntent is created with **manual capture** mode
3. Payment method is authorized (hold placed on customer's card)
4. PaymentIntent ID is stored with the order
5. Order is saved with status "CONFIRMED" (but payment not yet captured)

**Important Notes**:
- The customer's card is **authorized** but **not charged** at this point
- Funds are **reserved** but not transferred
- The order is confirmed and reserved for the customer
- PaymentIntent status will be `requires_capture`

**Error Responses**:
- **400 Bad Request**: 
  - `{"error": "Failed to place order: <error_message>"}`
  - Common errors:
    - "The selected offer is no longer active."
    - "The requested quantity exceeds available stock."
    - "The offer has expired."
    - "Payment authorization failed."
- **401 Unauthorized**: Token expired or invalid - redirect to login
- **500 Internal Server Error**: Server error - retry or show error message

**Implementation Notes**:
- Show user a message: "Order placed! Payment will be charged when you pick up your order."
- Store the order ID for later use
- The order should appear in "My Orders" with status "CONFIRMED" or "READY_FOR_PICKUP"
- Display a clear message that payment is authorized but not yet charged

---

## Stage 2: Capturing Payment on Pickup

When the customer picks up their order, the backend must **capture** the Stripe PaymentIntent.  
**Customer apps must not call** `POST /payment/capture/{paymentIntentId}` — that route accepts **vendor/admin** JWTs only (403 for customers).

### Confirm pickup (customer JWT) — use this

**Endpoint**: `PUT /orders/{orderId}/confirm-pickup`

**Headers**:
```
Authorization: Bearer <customer_jwt>
```

**Path parameters**:
- `orderId` (long): The order the logged-in customer is picking up

**Success (200 OK)** — capture is requested asynchronously (Kafka); order becomes `COMPLETED` after payment-service captures and emits `PaymentCapturedEvent`:
```json
{
  "success": true,
  "message": "Pickup confirmed. Payment capture has been requested."
}
```

**Error responses**:
- **400** — `INVALID_STATUS` or no Stripe PaymentIntent on the order (`NO_STRIPE_PAYMENT` messaging)
- **403** — order does not belong to the user
- **404** — order not found

**Implementation notes**:
- Call this when the customer confirms pickup (e.g. with staff at the counter).
- Refresh order details / poll until status is `COMPLETED`, or rely on push if you have it.
- Do **not** call `POST /payment/capture/...` with the customer token.

### Vendor/admin capture (optional)

Vendor dashboards may still use **`POST /payment/capture/{paymentIntentId}`** with a **vendor or admin** JWT (synchronous Stripe capture response).

**Kotlin example (customer flow)**:
```kotlin
suspend fun confirmPickup(orderId: Long): Result<ConfirmPickupResponse> {
    return try {
        val response = apiService.confirmPickup(orderId)
        if (response.success) {
            Result.success(response)
        } else {
            Result.failure(Exception(response.message ?: "Confirm pickup failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Usage when order is picked up (customer)
viewModelScope.launch {
    when (val result = confirmPickup(orderId)) {
        is Result.Success -> {
            showSuccessMessage("Pickup confirmed. Finalizing payment…")
            // Refresh order until status is COMPLETED (or handle via push notification)
            pollOrderUntilCompleted(orderId)
        }
        is Result.Failure -> {
            showErrorMessage("Failed to confirm pickup: ${result.exception.message}")
        }
    }
}
```

---

## Stage 3: Cancelling Payment (Order Cancellation)

If an order is cancelled before pickup, the payment authorization should be cancelled to release the hold on the customer's card.

### Cancel Payment

**Endpoint**: `POST /payment/cancel/{paymentIntentId}`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `paymentIntentId` (string, required): The Stripe PaymentIntent ID associated with the order

**Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Payment cancelled successfully",
  "paymentIntentId": "pi_1234567890abcdef",
  "status": "canceled"
}
```

**Error Responses**:
- **400 Bad Request**: 
  - `{"success": false, "message": "Failed to cancel payment: <error_message>"}`
  - Common errors:
    - "PaymentIntent not found"
    - "PaymentIntent already captured" (cannot cancel if already charged)
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: Server error - retry or show error message

**When to Call This Endpoint**:
- When customer cancels their order (before pickup)
- When vendor cancels the order
- When order is automatically cancelled (e.g., offer expired)
- **Before** marking the order as cancelled in your app

**Implementation Notes**:
- Call this endpoint when order is cancelled
- If cancellation fails, log the error but still mark order as cancelled
- The backend also automatically cancels payment when order is cancelled via the order service
- Display message: "Order cancelled. Payment authorization has been released."

**Kotlin Example**:
```kotlin
suspend fun cancelPayment(paymentIntentId: String): Result<PaymentCancelResponse> {
    return try {
        val response = apiService.cancelPayment(paymentIntentId)
        if (response.success) {
            Result.success(response)
        } else {
            Result.failure(Exception(response.message ?: "Payment cancellation failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Usage when order is cancelled
viewModelScope.launch {
    val order = getOrderById(orderId)
    order?.paymentIntentId?.let { paymentIntentId ->
        // Try to cancel payment, but don't block order cancellation if it fails
        cancelPayment(paymentIntentId).onFailure { error ->
            Log.e("Payment", "Failed to cancel payment: ${error.message}")
            // Still proceed with order cancellation
        }
    }
    
    // Cancel the order
    cancelOrder(orderId)
    showSuccessMessage("Order cancelled. Payment authorization released.")
}
```

---

## Order Status and Payment State

### Order Status Flow

```
PENDING → CONFIRMED → READY_FOR_PICKUP → COMPLETED
                ↓
            CANCELLED
```

### Payment State Flow

```
requires_capture (authorized, hold placed)
    ↓
succeeded (captured, customer charged)
    OR
canceled (cancelled, hold released)
```

### Mapping Order Status to Payment State

| Order Status | Payment State | Action Required |
|-------------|---------------|-----------------|
| PENDING | N/A | Wait for payment authorization |
| CONFIRMED | `requires_capture` | None - payment authorized, waiting for pickup |
| READY_FOR_PICKUP | `requires_capture` | None - ready to capture on pickup |
| COMPLETED | `succeeded` | None - payment captured |
| CANCELLED | `canceled` | None - payment cancelled |

---

## Getting Order Details with Payment Information

### Get Order by ID

**Endpoint**: `GET /orders/{orderId}`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response (200 OK)**:
```json
{
  "id": 123,
  "offerId": 456,
  "userId": 789,
  "quantity": 2,
  "unitPrice": 1500.00,
  "totalPrice": 3000.00,
  "vendorId": 101,
  "currency": "RSD",
  "paymentIntentId": "pi_1234567890abcdef",
  "status": "CONFIRMED",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Response Fields**:
- `id` (number): Order ID
- `paymentIntentId` (string, nullable): Stripe PaymentIntent ID (null for old orders)
- `status` (string): Order status (PENDING, CONFIRMED, READY_FOR_PICKUP, COMPLETED, CANCELLED)
- Other fields: Standard order fields

**Implementation Notes**:
- Check if `paymentIntentId` exists before attempting capture/cancel
- If `paymentIntentId` is null, the order was placed before manual capture was implemented
- For old orders without `paymentIntentId`, payment was already charged immediately

---

## Complete Implementation Example

### Order Placement Flow

```kotlin
class OrderViewModel : ViewModel() {
    private val apiService = ApiService()
    
    fun placeOrder(offerId: Long, quantity: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.placeOrder(
                    PlaceOrderRequest(offerId = offerId, quantity = quantity)
                )
                
                if (response.isSuccessful) {
                    // Order placed successfully
                    // Payment is authorized but not charged
                    _uiState.value = OrderUiState.Success(
                        message = "Order placed! Payment will be charged when you pick up your order."
                    )
                    
                    // Navigate to order details or order list
                    navigateToOrderDetails()
                } else {
                    // Handle error
                    _uiState.value = OrderUiState.Error(
                        message = response.errorBody()?.string() ?: "Failed to place order"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = OrderUiState.Error(
                    message = "Network error: ${e.message}"
                )
            }
        }
    }
}
```

### Pickup Flow (Capture Payment)

```kotlin
class PickupViewModel : ViewModel() {
    private val apiService = ApiService()
    
    fun confirmPickup(orderId: Long) {
        viewModelScope.launch {
            try {
                // 1. Get order details to retrieve paymentIntentId
                val orderResponse = apiService.getOrderById(orderId)
                if (!orderResponse.isSuccessful) {
                    _uiState.value = PickupUiState.Error("Failed to load order details")
                    return@launch
                }
                
                val order = orderResponse.body()
                if (order == null) {
                    _uiState.value = PickupUiState.Error("Order not found")
                    return@launch
                }
                
                // 2. Check if payment needs to be captured
                if (order.paymentIntentId != null && 
                    order.status == "READY_FOR_PICKUP" || order.status == "CONFIRMED") {
                    
                    // 3. Capture payment
                    val captureResponse = apiService.capturePayment(order.paymentIntentId)
                    if (!captureResponse.isSuccessful || 
                        !captureResponse.body()?.success == true) {
                        _uiState.value = PickupUiState.Error(
                            "Failed to process payment: ${captureResponse.body()?.message}"
                        )
                        return@launch
                    }
                }
                
                // 4. Mark order as completed
                val completeResponse = apiService.completeOrder(orderId)
                if (completeResponse.isSuccessful) {
                    _uiState.value = PickupUiState.Success("Order completed! Payment processed.")
                } else {
                    _uiState.value = PickupUiState.Error("Failed to complete order")
                }
                
            } catch (e: Exception) {
                _uiState.value = PickupUiState.Error("Error: ${e.message}")
            }
        }
    }
}
```

### Cancellation Flow

```kotlin
class OrderDetailsViewModel : ViewModel() {
    private val apiService = ApiService()
    
    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            try {
                // 1. Get order details
                val order = apiService.getOrderById(orderId).body()
                
                // 2. Cancel payment if order has paymentIntentId and is not yet captured
                order?.paymentIntentId?.let { paymentIntentId ->
                    if (order.status != "COMPLETED") {
                        // Try to cancel payment (non-blocking)
                        apiService.cancelPayment(paymentIntentId).onFailure { error ->
                            Log.e("Order", "Failed to cancel payment: ${error.message}")
                            // Continue with order cancellation even if payment cancel fails
                        }
                    }
                }
                
                // 3. Cancel the order
                val response = apiService.cancelOrder(orderId)
                if (response.isSuccessful) {
                    _uiState.value = OrderDetailsUiState.Success(
                        "Order cancelled. Payment authorization has been released."
                    )
                } else {
                    _uiState.value = OrderDetailsUiState.Error(
                        "Failed to cancel order"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = OrderDetailsUiState.Error("Error: ${e.message}")
            }
        }
    }
}
```

---

## API Service Interface

```kotlin
interface ApiService {
    // Order placement
    @POST("/orders/place-order")
    suspend fun placeOrder(
        @Body request: PlaceOrderRequest
    ): Response<PlaceOrderResponse>
    
    // Get order details
    @GET("/orders/{orderId}")
    suspend fun getOrderById(
        @Path("orderId") orderId: Long
    ): Response<OrderResponse>
    
    // Customer confirms pickup → triggers capture (use customer JWT)
    @PUT("/orders/{orderId}/confirm-pickup")
    suspend fun confirmPickup(
        @Path("orderId") orderId: Long
    ): Response<ConfirmPickupResponse>

    // Vendor/admin only — synchronous capture
    @POST("/payment/capture/{paymentIntentId}")
    suspend fun capturePayment(
        @Path("paymentIntentId") paymentIntentId: String
    ): Response<PaymentCaptureResponse>
    
    // Cancel payment
    @POST("/payment/cancel/{paymentIntentId}")
    suspend fun cancelPayment(
        @Path("paymentIntentId") paymentIntentId: String
    ): Response<PaymentCancelResponse>
    
    // Cancel order
    @POST("/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: Long
    ): Response<CancelOrderResponse>
}
```

### Data Classes

```kotlin
data class PlaceOrderRequest(
    val offerId: Long,
    val quantity: Int
)

data class PlaceOrderResponse(
    val message: String
)

data class OrderResponse(
    val id: Long,
    val offerId: Long,
    val userId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val vendorId: Long,
    val currency: String,
    val paymentIntentId: String?,
    val status: String,
    val createdAt: String
)

data class ConfirmPickupResponse(
    val success: Boolean,
    val message: String
)

data class PaymentCaptureResponse(
    val success: Boolean,
    val message: String,
    val paymentIntentId: String,
    val status: String
)

data class PaymentCancelResponse(
    val success: Boolean,
    val message: String,
    val paymentIntentId: String,
    val status: String
)
```

---

## UI/UX Considerations

### Order Placement Screen

**Display**:
- Order summary (items, quantity, total price)
- Clear message: "Payment will be authorized but not charged until pickup"
- Information icon explaining the payment hold
- "Place Order" button

**After Placement**:
- Success message: "Order placed! Payment authorization successful."
- Sub-message: "You will be charged when you pick up your order."
- Navigate to order details or order list

### Order Details Screen

**For CONFIRMED/READY_FOR_PICKUP Orders**:
- Show status: "Order Confirmed - Ready for Pickup"
- Display: "Payment authorized: [amount] (will be charged on pickup)"
- "Cancel Order" button (if cancellation is allowed)
- "I've Picked Up" button (for customer) or "Mark as Picked Up" (for vendor)

**For COMPLETED Orders**:
- Show status: "Order Completed"
- Display: "Payment processed: [amount]"
- No action buttons (order is complete)

**For CANCELLED Orders**:
- Show status: "Order Cancelled"
- Display: "Payment authorization released"
- No action buttons

### Pickup Confirmation Screen

**Before Capture**:
- Show order details
- Display: "Confirm pickup to process payment"
- "Confirm Pickup" button
- Loading indicator during payment capture

**After Capture**:
- Success message: "Payment processed successfully!"
- "Order Completed" confirmation
- Navigate back to order list

### Error Handling

**Payment Capture Failure**:
- Show error message: "Failed to process payment. Please try again."
- Keep order in "READY_FOR_PICKUP" status
- Allow retry
- Contact support option if retry fails

**Payment Cancellation Failure**:
- Log error but proceed with order cancellation
- Show message: "Order cancelled. If payment authorization doesn't release automatically, contact support."
- Don't block order cancellation

---

## Testing Checklist

### Order Placement
- [ ] Place order successfully
- [ ] Verify payment is authorized (not charged)
- [ ] Verify order status is CONFIRMED
- [ ] Verify paymentIntentId is stored in order

### Payment Capture
- [ ] Capture payment on pickup
- [ ] Verify payment status changes to "succeeded"
- [ ] Verify order status changes to COMPLETED
- [ ] Handle capture failure gracefully
- [ ] Don't capture if already captured

### Payment Cancellation
- [ ] Cancel order before pickup
- [ ] Verify payment is cancelled
- [ ] Verify payment status changes to "canceled"
- [ ] Handle cancellation failure gracefully
- [ ] Don't cancel if already captured

### Edge Cases
- [ ] Handle orders without paymentIntentId (old orders)
- [ ] Handle network failures during capture
- [ ] Handle expired authorization (7 days)
- [ ] Handle duplicate capture attempts
- [ ] Handle cancellation of already-captured payment

---

## Important Notes

### Authorization Expiry
- Stripe authorizations typically expire after **7 days**
- If order is not picked up within 7 days, authorization may expire
- Consider showing a warning if order is approaching expiry
- You may need to reauthorize if capture is attempted after expiry

### Backward Compatibility
- Orders placed before this feature was implemented won't have `paymentIntentId`
- For these orders, payment was already charged immediately
- Don't attempt to capture/cancel payments for orders without `paymentIntentId`
- Check for null `paymentIntentId` before payment operations

### Error Recovery
- If capture fails, the order remains in "READY_FOR_PICKUP" status
- Allow user to retry capture
- If multiple retries fail, contact support
- Log all payment operations for debugging

### Security
- Never expose PaymentIntent IDs in logs or error messages to end users
- Store PaymentIntent IDs securely
- Validate order ownership before allowing capture/cancel
- Use HTTPS for all API calls

---

## Summary

The Too Good To Go-style payment flow provides:

1. **Better Customer Protection**: Customers are only charged when they receive the order
2. **Flexibility**: Orders can be cancelled without charging the customer
3. **Trust**: Similar to trusted payment models like Too Good To Go
4. **Reduced Disputes**: Fewer chargebacks since customers are only charged on pickup

**Key Implementation Points**:
- Payment is **authorized** (not charged) at order placement
- Payment is **captured** (charged) when order is picked up
- Payment is **cancelled** (hold released) when order is cancelled
- Always check for `paymentIntentId` before payment operations
- Handle errors gracefully and allow retries

