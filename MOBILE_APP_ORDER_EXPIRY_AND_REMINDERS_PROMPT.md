# Mobile App AI Agent Prompt: Order Expiry, Pickup Reminders & No-Charge on Expiry

## Overview

You are the AI agent implementing **order expiry and pickup reminders** in the StillFresh Android app. The backend handles expiry and notifications; your job is to:

1. **Treat EXPIRED as a terminal status** – exclude EXPIRED orders from the basket; optionally show them in a dedicated section or with cancelled orders.
2. **Handle backend-driven notifications** – display or deep-link when the user receives “pick up by [time]” (1 hour before) and “Your reservation has expired”.
3. **Align with “pay on pickup”** – when an order expires, the user is **not charged**; the app should reflect that the reservation simply expired and no payment was taken.

---

## Backend Behavior (Summary)

- **Pickup deadline**: Each order has a `pickupBy` time (returned by the API when available). After this time, the backend may mark the order as **EXPIRED**.
- **Expiry job**: A scheduled job finds orders past `pickupBy` and:
  - Sets status to **EXPIRED**
  - Restores offer quantity
  - **Cancels the PaymentIntent** (same as user cancellation) – **no charge**
  - Publishes an event so the user gets a “Reservation expired” notification
- **Reminder**: About **1 hour before** `pickupBy`, the backend sends a notification: *“Your order must be picked up by [time].”*
- **Expired notification**: When an order is marked EXPIRED, the user receives a notification that the reservation has expired and that they were not charged.

---

## API & Data Model

### Order status values

Include **EXPIRED** in your status handling. Valid statuses:

- `PENDING`
- `CONFIRMED`
- `PROCESSING`
- `READY`
- `COMPLETED`
- `CANCELLED`
- **`EXPIRED`** – pickup window passed; order was not fulfilled; user was **not charged**.

### Order payload (relevant fields)

Ensure your `Order` / `OrderDto` model can represent:

- `id`, `offerId`, `userId`, `quantity`, `totalPrice`, `vendorId`, `currency`, `status`, `createdAt`, `updatedAt`
- **`pickupBy`** (optional, ISO-8601 date-time) – deadline by which the order must be picked up. Use this in the UI to show “Pick up by &lt;time&gt;” for active orders.

If the backend does not yet expose `pickupBy` in the list endpoint, you can still implement status handling and notifications; add `pickupBy` to the model when the API provides it.

---

## Product Requirements

### 1. Basket (active orders) must exclude EXPIRED

- **Basket / Current Reservations** must show only orders that are **not** in a terminal state:
  - Include: `PENDING`, `CONFIRMED`, `PROCESSING`, `READY`
  - **Exclude**: `COMPLETED`, `CANCELLED`, **`EXPIRED`**

Update your “active” filter from:

```kotlin
val activeStatuses = setOf("PENDING", "CONFIRMED", "PROCESSING", "READY")
```

to:

```kotlin
// EXPIRED is terminal; do not show in basket
val activeStatuses = setOf("PENDING", "CONFIRMED", "PROCESSING", "READY")
val terminalStatuses = setOf("COMPLETED", "CANCELLED", "EXPIRED")
val basketOrders = allOrders.filter { it.status in activeStatuses }
```

So **EXPIRED** orders must **never** appear in the basket list.

### 2. Where to show EXPIRED orders

Choose one (or both) of these approaches:

- **Option A – With cancelled**: Show EXPIRED in the same “Cancelled” tab/section as `CANCELLED`, with a clear label (e.g. “Expired” or “Expired (not charged)”).
- **Option B – Separate section**: Add a fourth section/tab “Expired” for `EXPIRED` orders only.

In both cases, make it clear to the user that **no payment was taken** for expired reservations (e.g. short text: “Reservation expired. You were not charged.”).

### 3. Backend-driven notifications (handling in the app)

The backend sends push notifications (via your existing FCM/notification pipeline):

- **“Pick up by [time]”** – sent about **1 hour before** the pickup deadline.  
  - **App action**: When the user opens the app from this notification (or sees it in the notification tray), consider deep-linking to the order detail or basket so they can see the order and the pickup time. Display the “pick up by” time prominently for active orders when you have `pickupBy`.
- **“Your reservation has expired”** – sent when the order is marked **EXPIRED**.  
  - **App action**: When the user opens from this notification, open the order list and ensure the order appears in the “Expired” or “Cancelled” section (not in the basket). Optionally open the specific order detail with a message like “This reservation expired. You were not charged.”

You do **not** need to implement the sending of these notifications; the backend does that. You **do** need to:

- Handle the notification types (e.g. `ORDER_PICKUP_REMINDER`, `ORDER_EXPIRED`) in your notification handler if you use data payloads for deep-linking.
- Refresh the order list when the user returns to the orders screen so EXPIRED orders appear in the correct section.

### 4. No charge on expiry (UX copy)

- In any screen that shows an **EXPIRED** order, include short, clear copy that **no charge was made** (e.g. “Reservation expired. You were not charged.”).
- Do **not** show a “payment taken” or “refund” for expired orders; the payment was never captured (same as cancellation). This aligns with “pay on pickup” and avoids legal/UX confusion.

---

## Implementation Checklist

1. **Status handling**
   - Add **EXPIRED** to your status enum/constants.
   - **Basket**: Exclude `EXPIRED` (and `COMPLETED`, `CANCELLED`) so only active statuses are shown.
   - **Cancelled / Expired section**: Include `EXPIRED` in the “Cancelled” list or in a dedicated “Expired” list; label clearly.

2. **Data model**
   - Add **`pickupBy`** to your order model if the API returns it; display “Pick up by &lt;time&gt;” in order detail and list items for active orders.

3. **Notifications**
   - In your FCM/notification handler, handle payloads for:
     - **Pickup reminder** – e.g. open orders/basket or order detail; show “Pick up by [time]” in UI if available.
     - **Order expired** – e.g. open orders list (or expired section) and refresh; show “Reservation expired. You were not charged.” where you display the order.

4. **Copy and UX**
   - For EXPIRED orders: always show that the user was **not charged**.
   - Do not imply a refund or charge for expired reservations.

5. **Optional: `GET /orders?status=EXPIRED`**
   - If the backend supports `GET /orders?status=EXPIRED`, you can use it to fetch only expired orders for a dedicated “Expired” tab; otherwise filter client-side from `GET /orders`.

---

## Summary

- **Expired logic**: Backend marks orders EXPIRED after `pickupBy`; restores quantity and **cancels PaymentIntent** (no charge).
- **Reminder**: Backend notifies user ~1 hour before pickup: “Your order must be picked up by [time].”
- **Expired notification**: Backend notifies user when reservation expires; message that they were not charged.
- **App**: Exclude EXPIRED from basket; show EXPIRED in Cancelled or dedicated section; handle reminder and expired notifications (deep-link + refresh); use clear “not charged” copy for expired orders.

Implement the above so the Android app correctly reflects expiry, reminders, and the no-charge-on-expiry behavior.
