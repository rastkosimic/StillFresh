# Android AI Agent Prompt: Vendor Dashboard Integration (Analytics + Active Orders + Payouts)

## Goal
Integrate the **Vendor Dashboard** into the StillFresh Android app. The dashboard is a **single aggregated endpoint** that returns:

- **Period summary** (units listed, units sold, **vendor earnings**, platform fee, gross sales, active orders count, **retail sell-through rate**)
- **Revenue trend** (daily vendor earnings for charting)
- **Sell-through trend** (daily units listed vs sold for STR chart)
- **Offer performance** (per-offer listed/sold + sell-through + earnings/fee)
- **Completed orders in period** (per-order platform fee, vendor net earnings, offer context)
- **Recent completed orders** (alias of `completedOrdersInPeriod` for backward compatibility)
- **Active orders** (quick operational list)
- **Ratings summary** (average + reviews count)
- **Payout balance** (unsettled balance + last payout info)

The backend is designed for **graceful degradation**: if one downstream service is unavailable, **that section may be `null`** while the rest still returns `200 OK`.

---

## Important: vendor earnings vs gross revenue

StillFresh retains a **platform fee** (default 10%, configurable by admin). Stats include **only COMPLETED orders** with a settlement snapshot.

| Metric | Field | Meaning |
|--------|-------|---------|
| **Your earnings** (primary) | `totalVendorEarningsCents` / `netAmountCents` | What the vendor actually earns after platform fee |
| **Platform fee** | `totalPlatformFeeCents` / `platformFeeCents` | StillFresh commission |
| **Gross sales** | `totalGrossRevenueCents` / `grossAmountCents` | What the customer paid |
| **Fee % applied** | `feePercentApplied` | Rate frozen at order settlement (audit when admin changes fee) |

**Deprecated:** `totalRevenue` / `revenue` — gross sales in major currency units (legacy alias). Do **not** label these as vendor profit.

All new financial fields use **minor currency units (cents)** unless noted.

---

## Retail sell-through rate (STR)

StillFresh uses the **Google retail model**: supply efficiency in the **same time window**.

```text
STR = (units completed in period / units listed in period) × 100
```

| Concept | Field | Source |
|---------|-------|--------|
| **Units listed** (denominator) | `totalUnitsListed`, `unitsListed` | Sum of supply listing events in period (create + replenish) |
| **Units sold** (numerator) | `totalUnitsSold`, `unitsSold` | Sum of `quantity` on **COMPLETED** orders settled in period |
| **Sell-through rate** | `sellThroughRate` | `unitsSold / unitsListed` as a **0–1 fraction** (multiply by 100 for %) |

**Rules:**
- Cancellations/rejections are excluded (only `COMPLETED` orders with settlement snapshot).
- Replenishment (vendor increases `quantityAvailable`) counts as newly listed supply.
- If `unitsListed == 0`, `sellThroughRate` is **`-1`** — display as `—` (not computable).
- Vendor summary STR is **`totalUnitsSold / totalUnitsListed`**, not an average of per-offer rates.
- `originalQuantity` is legacy (first publish size); **do not** use it for STR.

**Display:** `sellThroughRate >= 0` → `"${(sellThroughRate * 100).round()}%"`; `-1` → `"—"`.

---

## API Endpoint

### Get Vendor Dashboard (Aggregated)
```
GET /vendors/{vendorId}/dashboard?period=today|week|month|all&offerIds=101&offerIds=102
```

**Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Path params:**
- `vendorId` (Long): the vendor/location id of the authenticated vendor.

**Query params:**
- `period` (String, optional): one of `today`, `week` (default), `month`, or `all` (lifetime; no date filter on listed/sold/completed orders)
- `offerIds` (Long, optional, **repeatable**): filter analytics to selected offers. Omit for all offers (default). Empty list = all offers.

**Offer picker:** Load choices from `GET /vendors/all-offers`. Default selection = all offers (no `offerIds` param).

**Benchmark comparison:** Response always includes `periodBenchmark` (all offers for the same period). When filtering, compare `summary` (selected) vs `periodBenchmark` (total), e.g. “15 / 42 units sold this week” or STR 75% vs 62%.

**Important**
- Use the **vendor id from the authenticated session** (e.g., login response `vendor.id` / stored profile).
- Do **not** let users type the `vendorId`.

---

## Response Shape (Nullable Sections)

### Example response (200 OK)
```json
{
  "vendorId": 20,
  "period": "week",
  "selectedOfferIds": [101, 102],
  "generatedAt": "2026-03-18T12:34:56.789Z",
  "summary": {
    "totalUnitsSold": 15,
    "totalUnitsListed": 24,
    "totalVendorEarningsCents": 450000,
    "totalPlatformFeeCents": 50000,
    "totalGrossRevenueCents": 500000,
    "totalRevenue": 5000.0,
    "activeOrderCount": 1,
    "sellThroughRate": 0.625
  },
  "periodBenchmark": {
    "totalUnitsSold": 42,
    "totalUnitsListed": 68,
    "totalVendorEarningsCents": 1111050,
    "totalPlatformFeeCents": 123450,
    "totalGrossRevenueCents": 1234500,
    "totalRevenue": 12345.0,
    "activeOrderCount": 3,
    "sellThroughRate": 0.62
  },
  "sellThroughTrend": [
    {
      "date": "2026-03-12",
      "unitsListed": 10,
      "unitsSold": 8,
      "sellThroughRate": 0.8
    }
  ],
  "revenueTrend": [
    {
      "date": "2026-03-12",
      "vendorEarningsCents": 108000,
      "platformFeeCents": 12000,
      "grossRevenueCents": 120000,
      "revenue": 1200.0
    }
  ],
  "offerPerformance": [
    {
      "offerId": 101,
      "offerName": "Bread Basket",
      "originalQuantity": 20,
      "quantityAvailable": 5,
      "unitsListed": 30,
      "unitsSold": 15,
      "vendorEarningsCents": 80865,
      "platformFeeCents": 8985,
      "grossRevenueCents": 89850,
      "revenue": 898.5,
      "sellThroughRate": 0.75,
      "active": true
    }
  ],
  "completedOrdersInPeriod": [
    {
      "orderId": 9000,
      "offerId": 101,
      "offerName": "Bread Basket",
      "quantity": 2,
      "grossAmountCents": 120000,
      "platformFeeCents": 12000,
      "netAmountCents": 108000,
      "feePercentApplied": 10.0,
      "currency": "RSD",
      "settledAt": "2026-03-18T10:15:00+01:00"
    }
  ],
  "recentCompletedOrders": [
    {
      "orderId": 9000,
      "offerId": 101,
      "offerName": "Bread Basket",
      "quantity": 2,
      "grossAmountCents": 120000,
      "platformFeeCents": 12000,
      "netAmountCents": 108000,
      "feePercentApplied": 10.0,
      "currency": "RSD",
      "settledAt": "2026-03-18T10:15:00+01:00"
    }
  ],
  "activeOrders": [
    {
      "orderId": 9001,
      "offerId": 101,
      "quantity": 2,
      "totalPrice": 11.98,
      "currency": "RSD",
      "status": "READY",
      "pickupBy": "2026-03-18T18:00:00+01:00",
      "paymentMethod": "CARD"
    }
  ],
  "ratings": {
    "averageRating": 4.6,
    "reviewsCount": 128
  },
  "payoutBalance": {
    "unsettledCents": 125000,
    "currency": "RSD",
    "lastPayoutAmountCents": 50000,
    "lastPayoutAt": "2026-03-01T09:00:00Z"
  }
}
```

### Nullable behavior
Any of these can be missing/`null`:
- `summary`, `periodBenchmark`, `revenueTrend`, `sellThroughTrend`, `offerPerformance`, `completedOrdersInPeriod`, `recentCompletedOrders`, `activeOrders`, `ratings`, `payoutBalance`

When no offer filter is applied, `selectedOfferIds` is `null` and `summary` matches `periodBenchmark`.

Invalid `offerIds` (not owned by vendor) → `400 Bad Request` with `{ "error": "..." }`.

Your UI must handle nulls and show “Unavailable right now” placeholders per section (not a full-screen error) when the overall request succeeds.

---

## Completed order detail (order API)

When displaying a **COMPLETED** order (order detail / history), the `Order` JSON includes:

| Field | Type | Description |
|-------|------|-------------|
| `grossAmountCents` | Long? | Customer paid (null until settled) |
| `platformFeeCents` | Long? | Platform fee for this order |
| `netAmountCents` | Long? | Vendor earnings for this order |
| `feePercentApplied` | Double? | Fee % at settlement |
| `settledAt` | String? | ISO timestamp when payment was captured |

Show **net amount** as “Your earnings” and **platform fee** separately. Display `feePercentApplied` as “Platform fee (X%)”.

---

## Kotlin Data Models (Jetpack Compose friendly)

```kotlin
data class VendorDashboardResponse(
    val vendorId: Long,
    val period: String,
    val selectedOfferIds: List<Long>? = null,
    val generatedAt: String,
    val summary: PeriodSummary? = null,
    val periodBenchmark: PeriodSummary? = null,
    val revenueTrend: List<DailyRevenueStat>? = null,
    val sellThroughTrend: List<SellThroughDailyStat>? = null,
    val offerPerformance: List<OfferPerformance>? = null,
    val completedOrdersInPeriod: List<CompletedOrderSummary>? = null,
    val recentCompletedOrders: List<CompletedOrderSummary>? = null,
    val activeOrders: List<ActiveOrder>? = null,
    val ratings: RatingSummary? = null,
    val payoutBalance: PayoutSummary? = null
)

data class PeriodSummary(
    val totalUnitsSold: Long,
    val totalUnitsListed: Long = 0,
    val totalVendorEarningsCents: Long = 0,
    val totalPlatformFeeCents: Long = 0,
    val totalGrossRevenueCents: Long = 0,
    val totalRevenue: Double = 0.0, // deprecated gross alias
    val activeOrderCount: Int,
    val sellThroughRate: Double = -1.0 // 0–1 fraction; -1 if not computable
)

data class SellThroughDailyStat(
    val date: String,
    val unitsListed: Long = 0,
    val unitsSold: Long = 0,
    val sellThroughRate: Double = -1.0
)

data class DailyRevenueStat(
    val date: String,
    val vendorEarningsCents: Long = 0,
    val platformFeeCents: Long = 0,
    val grossRevenueCents: Long = 0,
    val revenue: Double = 0.0 // deprecated gross alias
)

data class OfferPerformance(
    val offerId: Long,
    val offerName: String?,
    val originalQuantity: Int, // legacy first-publish size; do not use for STR
    val quantityAvailable: Int,
    val unitsListed: Long = 0,
    val unitsSold: Long = 0,
    val vendorEarningsCents: Long = 0,
    val platformFeeCents: Long = 0,
    val grossRevenueCents: Long = 0,
    val revenue: Double = 0.0,
    val sellThroughRate: Double = -1.0, // unitsSold / unitsListed; -1 if unitsListed == 0
    val active: Boolean
)

data class CompletedOrderSummary(
    val orderId: Long?,
    val offerId: Long? = null,
    val offerName: String? = null,
    val quantity: Int = 0,
    val grossAmountCents: Long,
    val platformFeeCents: Long,
    val netAmountCents: Long,
    val feePercentApplied: Double?,
    val currency: String?,
    val settledAt: String?
)

data class ActiveOrder(
    val orderId: Long?,
    val offerId: Long?,
    val quantity: Int,
    val totalPrice: Double,
    val currency: String?,
    val status: String?,
    val pickupBy: String?,
    val paymentMethod: String?
)

data class RatingSummary(
    val averageRating: Double,
    val reviewsCount: Int
)

data class PayoutSummary(
    val unsettledCents: Long,
    val currency: String?,
    val lastPayoutAmountCents: Long? = null,
    val lastPayoutAt: String? = null
)
```

**Currency formatting helper:**
```kotlin
fun formatCents(cents: Long, currency: String = "RSD"): String {
    val major = cents / 100.0
    return "%.2f %s".format(major, currency)
}
```

---

## UI/UX Requirements (Compose)

### Summary cards (use earnings, not gross)
1. **Your earnings** — `summary.totalVendorEarningsCents` (primary headline metric)
2. **Benchmark** — show vs `periodBenchmark` (e.g. earnings, units sold, STR for all offers in period)
3. **Platform fee** — `summary.totalPlatformFeeCents` (secondary, e.g. “StillFresh fee”)
4. **Gross sales** (optional) — `summary.totalGrossRevenueCents`
5. **Units listed / sold** — `summary.totalUnitsListed`, `summary.totalUnitsSold` (compare to benchmark)
6. **Sell-through %** — `summary.sellThroughRate * 100` vs benchmark (show `—` when `-1`)
7. Active orders count (filtered when offerIds present)

### Offer filter UI
- Multi-select from `GET /vendors/all-offers` (include inactive offers)
- Default: all offers (no `offerIds` in request)
- On change, re-fetch dashboard with `offerIds=...` repeated params
- `offerPerformance` returns only selected offers when filtered; all offers when unfiltered

### Sell-through trend chart (optional)
- Plot `sellThroughTrend`: daily `unitsListed` vs `unitsSold`, or line chart of `sellThroughRate * 100`

### Offer performance table
- Show `unitsListed`, `unitsSold`, STR per offer
- Do not derive STR from `originalQuantity`

### Completed orders in period
- Prefer `completedOrdersInPeriod` (same data as `recentCompletedOrders`)
- Show offer name, quantity, net earnings, platform fee, fee %

### Revenue trend chart
- Plot **`vendorEarningsCents`** per day (not gross `revenue`)
- Optionally show platform fee as stacked area or separate line

### Recent completed orders
- Same list as `completedOrdersInPeriod`: order id, offer, quantity, net earnings, platform fee, fee %

---

## Testing Checklist
- [ ] Summary shows vendor earnings, not gross, as primary metric
- [ ] `periodBenchmark` always present; equals `summary` when no offer filter
- [ ] Multi-select offer filter sends repeated `offerIds` params
- [ ] Filtered summary differs from benchmark when subset selected
- [ ] Invalid offer id returns 400
- [ ] STR uses `totalUnitsListed` / `unitsListed`, not `originalQuantity`
- [ ] `sellThroughRate == -1` renders as `—`
- [ ] Period `all` loads lifetime listed/sold/completed orders
- [ ] Per completed order shows `offerId`, `offerName`, `quantity`, `platformFeeCents`, `netAmountCents`, `feePercentApplied`
- [ ] `totalRevenue` / `revenue` treated as deprecated gross alias only
- [ ] Cents formatted correctly (divide by 100)
- [ ] Handle null sections gracefully

---

## Summary
Implement a vendor dashboard backed by `GET /vendors/{vendorId}/dashboard?period=...`. Display **vendor net earnings** and **platform fee** separately; show **retail STR** as units sold ÷ units listed in the same period; never label gross sales as vendor profit.
