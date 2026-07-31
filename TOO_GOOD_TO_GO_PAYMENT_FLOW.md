# Too Good To Go Style Payment Flow Implementation

This document describes the implementation of a Too Good To Go-style payment flow where payments are authorized (held) at order placement and only captured when the order is picked up.

## Overview

The payment flow now works as follows:

1. **Order Placement**: PaymentIntent is created with **manual capture** - this places a hold on the customer's card but doesn't charge immediately
2. **Order Confirmation**: Order is saved with the PaymentIntent ID
3. **Pickup**: PaymentIntent is captured (customer is charged)
4. **Cancellation**: If order is cancelled, PaymentIntent is cancelled (hold is released)

## Key Changes

### 1. Payment Service (`payment-service`)

#### Modified `PaymentService.processPaymentRequest()`
- Changed from immediate charge to manual capture
- Uses `setCaptureMethod(CaptureMethod.MANUAL)` 
- PaymentIntent status will be `requires_capture` after authorization
- Stores PaymentIntent ID in PaymentSuccessEvent

#### New Methods
- `capturePaymentIntent(String paymentIntentId)`: Captures a PaymentIntent (charges the customer)
- `cancelPaymentIntent(String paymentIntentId)`: Cancels a PaymentIntent (releases the hold)

#### Endpoints (`PaymentController`)
- `POST /payment/capture/{paymentIntentId}`: Capture payment (**vendor/admin JWT only**)
- `POST /payment/cancel/{paymentIntentId}`: Cancel payment (vendor/admin; customer cancels go through order service)

#### New Listeners
- `PaymentCancelRequestListener`: Listens for payment cancellation requests from order service
- `PaymentCaptureRequestListener`: Listens for customer pickup confirmations and captures the PaymentIntent

### 2. Order Service (`order-service`)

#### Modified `Order` Entity
- Added `paymentIntentId` field to store Stripe PaymentIntent ID

#### Modified `OrderService.finalizeOrder()`
- Now accepts and stores `paymentIntentId` parameter
- Stores PaymentIntent ID in the order record

#### Methods
- `cancelOrderById(...)`: Cancels an existing order and publishes `PaymentCancelRequestEvent`
- `requestCustomerPickupCapture(orderId, userId)`: Validates customer + order state, publishes `PaymentCaptureRequestEvent`

#### `OrderController`
- `PUT /orders/{id}/confirm-pickup`: **Customer (or super admin)** confirms pickup; triggers async Stripe capture (use this instead of `POST /payment/capture/...` with a customer token)

#### Modified `OrderEventPublisher`
- `publishPaymentCancelRequest()` — cancel authorized payment
- `publishPaymentCaptureRequest()` — capture after customer confirms pickup

### 3. Shared Entities (`shared-entities`)

#### Modified `PaymentSuccessEvent`
- Added `paymentIntentId` field
- Updated constructors to accept paymentIntentId

#### Events
- `PaymentCancelRequestEvent`: Order cancelled → release card hold
- `PaymentCaptureRequestEvent`: Customer confirmed pickup → capture PaymentIntent

### 4. Database Migration

#### New Migration Script
- `order-service/add_payment_intent_id_column.sql`: Adds `payment_intent_id` column to `orders` table

## Usage

### 1. Order Placement Flow (Automatic)

The flow is automatic - when an order is placed:
1. PaymentIntent is created with manual capture
2. PaymentIntent ID is stored in the order
3. Order status is "reserved" (payment authorized but not captured)

### 2. Capturing Payment on Pickup

**Mobile / customer app (customer JWT):** confirm pickup on the **order** — the backend checks the order belongs to the user and enqueues capture:

```bash
PUT /orders/{orderId}/confirm-pickup
```

```bash
curl -X PUT http://localhost:8080/orders/42/confirm-pickup \
  -H "Authorization: Bearer <customer_token>"
```

**Response (accepted; capture runs asynchronously via Kafka):**
```json
{
  "success": true,
  "message": "Pickup confirmed. Payment capture has been requested."
}
```

The order moves to **COMPLETED** when `payment-service` captures successfully and publishes `PaymentCapturedEvent` (existing listener).

**Vendor dashboard (vendor/admin JWT):** may still call:

```bash
POST /payment/capture/{paymentIntentId}
```

### 3. Cancelling Payment on Order Cancellation

When an order is cancelled, the system automatically:
1. Publishes `PaymentCancelRequestEvent` via Kafka
2. Payment service listens and cancels the PaymentIntent
3. Hold on customer's card is released

You can also manually cancel a payment:

```bash
POST /payment/cancel/{paymentIntentId}
```

**Example:**
```bash
curl -X POST http://localhost:8080/payment/cancel/pi_1234567890 \
  -H "Authorization: Bearer <token>"
```

**Response:**
```json
{
  "success": true,
  "message": "Payment cancelled successfully",
  "paymentIntentId": "pi_1234567890",
  "status": "canceled"
}
```

### 4. Cancelling an Order Programmatically

To cancel an order by ID (which will also cancel the payment):

```java
orderService.cancelOrderById(orderId);
```

## PaymentIntent Status Flow

1. **Created**: `requires_capture` - Payment authorized, hold placed on card
2. **Captured**: `succeeded` - Payment charged to customer
3. **Cancelled**: `canceled` - Hold released, no charge

## Important Notes

### Authorization Expiry
- Stripe authorizations typically expire after 7 days
- Ensure you capture the payment before the authorization expires
- If needed, you can reauthorize before capturing

### Error Handling
- If capture fails, the order can still be fulfilled (payment was authorized)
- If cancellation fails, log the error and handle manually if needed
- Always check PaymentIntent status before attempting capture/cancel

### Testing
- Use Stripe test mode to test the flow
- Test cards: Use `4242 4242 4242 4242` for successful authorization
- Check PaymentIntent status in Stripe Dashboard

## Database Migration

Run the migration script to add the `payment_intent_id` column:

```sql
-- Run this on your order database
\i order-service/add_payment_intent_id_column.sql
```

Or manually:
```sql
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(255);
```

## Kafka Topics

The following Kafka topics are used:

- `payment-cancel-request`: Payment cancellation (order service → payment service)
- `payment-capture-request`: Customer pickup confirmed → capture PaymentIntent (order service → payment service)

Kafka will auto-create topics if enabled; otherwise create them explicitly.

## Configuration

No additional configuration is required. The existing payment service configuration works with manual capture.

## Benefits

1. **Customer Protection**: Customers are only charged when they actually receive the order
2. **Flexibility**: Orders can be cancelled without charging the customer
3. **Better UX**: Similar to Too Good To Go's trusted payment model
4. **Reduced Disputes**: Fewer chargebacks since customers are only charged on pickup

## Future Enhancements

- Add automatic capture before authorization expiry
- Add webhook handlers for PaymentIntent status changes
- Add retry logic for failed captures
- Add monitoring/alerting for uncaptured payments

