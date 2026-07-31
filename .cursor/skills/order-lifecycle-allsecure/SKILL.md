---
name: order-lifecycle-allsecure
description: >-
  Manages the StillFresh order lifecycle (Reserved/Pickup/Cancel) and AllSecure payment 
  integration. Use when modifying order-service states, payment-service providers, Kafka 
  events for preauth/capture/void, or implementing Serbian domestic payment and anti-bypass workflows.
---

# Order Lifecycle & Fraud Prevention (AllSecure)

## Current vs Target

**Keep this table updated** whenever order/payment/fraud code changes. For the full as-is event
pipeline and DB inventory, see
[`ORDER_LIFECYCLE_ALLSECURE_SUMMARY.md`](../../../ORDER_LIFECYCLE_ALLSECURE_SUMMARY.md) at the repo root.
For **local Windows + ngrok** setup, see [`ALLSECURE_LOCAL_DEV.md`](../../../ALLSECURE_LOCAL_DEV.md).

| Area | Status | Notes |
|------|--------|-------|
| **Reserved** | **Implemented** | `POST /orders/place-order` → preauthorize → `orders.status=CONFIRMED`; stock decremented; push via `OrderPlacedEvent`. |
| **Reservation nudge email** | **Implemented (opt-in)** | Serbian copy sent from `notification-service` `OrderPlacedConsumer` via `EmailService` (Mailgun). Gated by `notification.email.enabled` (default `false`). `customerEmail` threaded through `OrderRequestEvent` → `OrderPlacedEvent`. |
| **Pickup (Kafka)** | **Implemented** | `PaymentCaptureRequestEvent` → active provider capture → ledger → `PaymentCapturedEvent` → `COMPLETED`. Works for AllSecure and Stripe. |
| **Pickup (HTTP)** | **Implemented** | `PUT /orders/{id}/confirm-pickup` is provider-neutral (`NO_PAYMENT_REFERENCE` when no hold). `POST /payment/capture/{id}` routes through `PaymentProviderRouter` (not Stripe-only). |
| **Cancel + void** | **Implemented** | `PUT /orders/{id}/cancel` → `CANCELLED`, stock restored, void via `PaymentCancelRequestEvent`. |
| **Geo-fence fraud** | **Implemented** | `userLat`/`userLon` on cancel body; `GeoFenceService` Haversine &lt;50 m within pickup window → `FraudFlagEvent`. Cancellation **always proceeds** (coords optional). |
| **Bypass strikes** | **Implemented** | `bypass_strike_count` on `users` and `vendors`; incremented via `FraudFlagEvent` consumers in `user-service` / `vendor-service`. |
| **Expiry / no-show** | **Implemented** | `OrderExpiryScheduler` → `EXPIRED`, stock restored, void, `OrderExpiredEvent` + `OrderNoShowEvent`. |
| **No-show strikes + suspend** | **Implemented** | `no_show_strike_count` on `users`; combined bypass+no-show ≥3 → `Status.SUSPENDED` + `UserSuspendedEvent`. |
| **Session termination** | **Partial** | `authorization-service` revokes all refresh tokens on `UserSuspendedEvent`. **TODO:** per-jti access-token blacklist so in-flight JWTs die immediately. |
| **Ledger / fees** | **Implemented** | `ledger_entries` on capture (`VENDOR_CREDIT` / `PLATFORM_FEE_INCOME`). Fee is **runtime-customizable** via `platform_settings` (default **10%**), `GET`/`PUT /admin/platform/fee`, cached in Redis. Fee read at preauthorize and stored on `payment_transactions.platform_fee_cents`. |
| **Fiscal (e-fiskalni)** | **Stub only** | `FiscalReceiptService` + `HttpFiscalReceiptService` fired from `LedgerService.writePaymentLedger` after settlement. Disabled by default (`fiscal.enabled=false`). **TODO:** real certified POS contract ("Prodaja preko posrednika"). |
| **Card on file + order 3DS** | **Implemented** | AllSecure register + `customer_payment_methods`. Order-time 3DS: `payment_transactions.authorization_status` + `redirect_url`; poll `GET /payment/allsecure/payment-status/{requestId}` → `AUTHENTICATION_REQUIRED` exposes `redirectUrl`. |
| **B2B vendor payouts** | **Implemented** | Ledger pipeline + swappable `PayoutRail` (`stub` / `sepa-xml` / `cmiplus`). Auto approve+submit by default; admin override via `/ledger/payouts/*`. See [`CMIPLUS_PAYOUT_INTEGRATION.md`](../../../CMIPLUS_PAYOUT_INTEGRATION.md). |

### Remaining gaps (target not yet met)

1. **Cancel coords:** backend accepts optional `userLat`/`userLon`; does not reject cancel when omitted (fraud check skipped only).
2. **Email:** enable in prod (`NOTIFICATION_EMAIL_ENABLED=true` + Mailgun env vars).
3. **Fiscal:** replace `HttpFiscalReceiptService` placeholder with vendor POS / certified e-fiskalni integration.
4. **JWT:** blacklist outstanding access tokens on suspension (not only refresh-token rotation).
5. **Vendor suspension:** vendors accumulate `bypass_strike_count` but are not auto-suspended yet (ops/monitoring only).

---

## Instructions

1. **Map business states accurately:**
   - Business "Reserved" = DB `CONFIRMED` (AllSecure preauth hold active).
   - Business "Picked Up" = DB `COMPLETED` (funds captured).
   - Business "Cancelled" / "Expired" = DB `CANCELLED` / `EXPIRED` (preauth voided).
2. **Provider-neutral capture:** Never gate pickup on `pi_` prefix. Use `paymentIntentId` column as the active provider's authorization reference (Stripe `pi_…` or AllSecure UUID). Route HTTP capture through `PaymentProviderRouter`.
3. **Fees:** Read platform fee from `PlatformSettingsService.getFeePercent()` at preauthorize time; do not hardcode or inject static `@Value` fee percentages.
4. **Serbia compliance & nudges:** Ledger split on capture; optional reservation email (Serbian copy); fiscal receipt stub after ledger write; geo-fence on cancel publishes `FraudFlagEvent` when &lt;50 m inside pickup window.
5. **Strikes:** `FraudFlagEvent` → user + vendor bypass counters; `OrderNoShowEvent` → user no-show counter; ≥3 combined active strikes → `SUSPENDED` + `UserSuspendedEvent` → refresh-token revocation.

## Key Files

| Concern | Path |
|---------|------|
| Order lifecycle | `order-service/.../OrderService.java`, `OrderExpiryScheduler.java` |
| Geo-fence / fraud flag | `order-service/.../GeoFenceService.java` |
| AllSecure provider | `payment-service/.../AllSecurePaymentProvider.java`, `AllSecureController.java` (`/payment-status/{requestId}`) |
| Capture listener | `payment-service/.../PaymentCaptureRequestListener.java` |
| Provider router | `payment-service/.../PaymentProviderRouter.java` |
| Platform fee admin | `payment-service/.../PlatformSettingsService.java`, `PlatformSettingsController.java` |
| Ledger + fiscal hook | `payment-service/.../LedgerService.java`, `FiscalReceiptService.java` |
| User strikes / suspend | `user-service/.../StrikeService.java`, `OrderStrikeListener.java` |
| Vendor bypass strikes | `vendor-service/.../VendorFraudFlagListener.java` |
| Session revoke | `authorization-service/.../UserSuspensionListener.java`, `RefreshTokenService.java` |
| Reservation email | `notification-service/.../OrderPlacedConsumer.java`, `EmailService.java` |
| Shared events | `shared-entities/.../FraudFlagEvent.java`, `OrderNoShowEvent.java`, `UserSuspendedEvent.java` |

---

## Technical Protocols & Event Flows

### PROTOCOL A: Order Reserved (Placement Flow)
**Trigger:** User sends `POST /orders/place-order` via `user-service`.
*   **Prerequisite:** A valid card token must already exist in `customer_payment_methods.reference_id`.

#### Event Pipeline:
1. `user-service` publishes `OrderRequestEvent` to Kafka (includes `customerEmail`).
2. `order-service` validates the item via `OfferDetailsRequestedEvent` / `OfferDetailsResponseEvent`.
3. `order-service` publishes `PaymentRequestEvent` to Kafka.
4. `payment-service` invokes `AllSecurePaymentProvider.preauthorize()` with `transactionIndicator=CARDONFILE`. If AllSecure returns a 3DS `redirectUrl`, set `authorization_status=AUTHENTICATION_REQUIRED` and persist `redirect_url`; clients poll `GET /payment/allsecure/payment-status/{requestId}` until `AUTHORIZED` or `FAILED`.
5. `payment-service` maps the AllSecure UUID to `payment_intent_id` in `payment_transactions` (fee from `platform_settings` at this step).
6. Upon a successful authorize callback, `payment-service` publishes `PaymentSuccessEvent`.
7. `order-service` executes `finalizeOrder()`, inserting a row into `orders` with `status=CONFIRMED`, `pickup_by`, and geo snapshot (`latitude`/`longitude` from offer).
8. `order-service` emits `OfferQuantityUpdatedEvent` (negative quantity) and fires `OrderPlacedEvent` (includes `customerEmail`).
9. **Psychological Nudge Email (opt-in):** `notification-service` sends Serbian copy when `notification.email.enabled=true`: *"Sredstva su uspešno rezervisana…"* (see `OrderPlacedConsumer`).

---

### PROTOCOL B: Order Picked Up (Capture & Settlement)
**Trigger:** `PUT /orders/{id}/confirm-pickup` (customer) or vendor/admin capture path.

#### Event Pipeline:
1. `order-service` publishes `PaymentCaptureRequestEvent` with the authorization reference (`paymentIntentId` column).
2. `payment-service` intercepts via `PaymentCaptureRequestListener` and routes to the active provider's `capture(referenceId)` (AllSecure or Stripe).
3. `payment-service` executes ledger settlement via `LedgerService.writePaymentLedger()`:
   - Writes `ledger_entries`: `VENDOR_CREDIT` (net) and `PLATFORM_FEE_INCOME` (fee stored on `payment_transactions.platform_fee_cents`).
   - Sets `payment_transactions.ledger_written = true`.
   - Calls `FiscalReceiptService.issueReceipt()` (stub; disabled by default).
4. `payment-service` publishes `PaymentCapturedEvent`.
5. `order-service` intercepts `PaymentCapturedEvent` and changes order status to `COMPLETED`.
6. **Fiscal (TODO):** Replace stub with webhook to vendor POS/ERP for certified Serbian e-fiskalni račun ("Prodaja preko posrednika").

---

### PROTOCOL C: Order Cancelled & Anti-Bypassing Fraud Engine
**Trigger:** User triggers a cancellation request via `PUT /orders/{id}/cancel`.

#### Event Pipeline & Logic:
1. `order-service` verifies the order is in `CONFIRMED`, `PROCESSING`, or `READY` state.
2. **Cancel body:** optional `userLat` / `userLon`. Backend does not block cancel when coords are missing (only skips fraud check).
3. **Fraud Interception Engine (when coords present):**
   - Compare user coords to order snapshot `latitude`/`longitude` (from offer at placement).
   - Haversine distance via `GeoFenceService`.
   - *Rule:* If `distance < 50 m` AND current time is within the active pickup window → publish `FraudFlagEvent` (`potential_bypass_fraud`). Increment `bypass_strike_count` for user (via `user-service`) and vendor (via `vendor-service`).
4. `order-service` changes the row status to `CANCELLED` (regardless of fraud flag).
5. `order-service` fires `OfferQuantityUpdatedEvent` (+qty) to restore active stock.
6. `order-service` publishes `PaymentCancelRequestEvent` to Kafka.
7. `payment-service` executes active provider `cancel()`, issuing void on the pre-auth reference.

---

## 3. AUTOMATED BACKGROUND TASKS (MISSING PICKUP)

### Cron Job Scheduler: `OrderExpiryScheduler` (Runs every 10 minutes)
1. Queries `stillfresh_orderdb.orders` for rows where status is `CONFIRMED`, `PROCESSING`, or `READY` AND the current time has surpassed the `pickup_by` deadline.
2. Updates `orders.status = EXPIRED`.
3. Emits `OfferQuantityUpdatedEvent` (+qty) to release inventory.
4. Emits `PaymentCancelRequestEvent` to void the pre-auth hold.
5. Publishes `OrderNoShowEvent` → `user-service` increments `no_show_strike_count`.
6. Publishes `OrderExpiredEvent` for push notification.
7. *Account suspension:* Combined bypass + no-show strikes ≥ 3 → `Status.SUSPENDED`, `UserSuspendedEvent` → `authorization-service` revokes refresh tokens. **TODO:** per-jti access-token blacklist.
