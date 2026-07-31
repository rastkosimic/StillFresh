# Mobile App AI Agent Prompt: Pickup Slot Sections (Collect now / today / tomorrow / lunch / dinner)

## Goal
Implement **pickup-time slot sections** on the Android Discover screen exactly like the screenshots:
- **Collect now**
- **Collect tomorrow** (and optionally **Collect today**)
- **Collect for lunch**
- **Collect for dinner**
Each section is a horizontal carousel with a **See all** action.

**Important:** All slot computation is already done on the backend in the **vendor’s timezone inferred from coordinates**, so the Android app should **NOT** compute “today/now/lunch/dinner” locally. Use the fields from the API response.

---

## Data Model (OfferDto fields to use)
When you fetch offers, each offer includes:
- `pickupDate` (string, `YYYY-MM-DD`) — vendor-selected pickup date
- `pickupStartTime` (string, `HH:mm:ss`) — vendor-local time
- `pickupEndTime` (string, `HH:mm:ss`) — vendor-local time

Derived fields (already computed server-side in vendor timezone):
- `pickupDaySlot`: `TODAY | TOMORROW | FUTURE | PAST`
- `pickupMealSlot`: `BREAKFAST | LUNCH | DINNER | OTHER`
- `collectNow`: boolean (true when vendor-local now is within pickup window for vendor-local today)

Also available:
- `category`: `MEALS | BREAD_PASTRIES | GROCERIES | FLOWERS_PLANTS | PET_FOOD`

---

## APIs to call (via API Gateway)
Use the Offer Service endpoints through the gateway:

### 1) Nearby offers (recommended for Discover screen)
`GET /offers/nearby?latitude={lat}&longitude={lon}&range={km}&category={optionalOfferCategory}`

This returns a list of `OfferDto` including the pickup slot fields above.

### 2) Optional: Filter by product category server-side
`GET /offers/nearby?...&category=BREAD_PASTRIES`

### 3) Categories list (localized display names)
`GET /offers/categories?locale={locale}`  
(Already implemented earlier; use for product category chips, not pickup slots.)

---

## Rendering Plan (match screenshots)
On the **Discover** screen:

### Section order (recommended)
1) **Collect now** (highest intent)
2) **Collect today** (optional; if you want parity with some screenshots)
3) **Collect tomorrow**
4) **Collect for lunch**
5) **Collect for dinner**
6) (Optional) product category sections: **Baked goods**, **Groceries**, etc. using `category`

### Filtering rules per section
Assume you already fetched `nearbyOffers` (List<OfferDto>) once.

#### Collect now
Include offers where:
- `offer.collectNow == true`
Suggested sort:
- soonest pickup end first (by `pickupEndTime`)

#### Collect today (optional)
Include offers where:
- `offer.pickupDaySlot == TODAY`
- `offer.collectNow == false` (to avoid duplicates with “Collect now”)
Suggested sort:
- soonest pickup start first (by `pickupStartTime`)

#### Collect tomorrow
Include offers where:
- `offer.pickupDaySlot == TOMORROW`
Suggested sort:
- soonest pickup start first

#### Collect for lunch
Include offers where:
- `offer.pickupMealSlot == LUNCH`
- `offer.pickupDaySlot in {TODAY, TOMORROW}` (optional; you can include FUTURE too if you want)
Suggested sort:
- TODAY before TOMORROW, then by pickupStartTime

#### Collect for dinner
Include offers where:
- `offer.pickupMealSlot == DINNER`
- `offer.pickupDaySlot in {TODAY, TOMORROW}` (optional)

### Duplicates policy
It’s OK if the same offer appears in multiple sections (TooGoodToGo-style), but if you want to avoid “spam”:
- Keep “Collect now” unique (exclude those from other sections)
- Allow overlap between “tomorrow” and “dinner” etc., or dedupe by offerId per section list.

---

## UI Components (Jetpack Compose)

### Section header
Each section:
- Title (e.g., “Collect now”)
- “See all” button

### Section list
Use `LazyRow` of offer cards.

### Offer card contents (match screenshots)
- Offer image (`imageUrl`)
- Vendor name
- Offer name/description
- Pickup time text: e.g. `Collect today: 19:00 - 21:00`
- Distance (if you compute it client-side from user coords and offer coords)
- Price + old price (if you show strike-through original price)
- Favorite icon

---

## “See all” behavior
When user taps **See all** on a section:

Navigate to a list screen with a filter key, e.g.:
- `PickupSection.COLLECT_NOW`
- `PickupSection.TOMORROW`
- `PickupSection.LUNCH`
- `PickupSection.DINNER`

On that screen:
- Either reuse the same `nearbyOffers` already fetched (fast, consistent)
- Or refetch `/offers/nearby` to ensure the server-side slots are fresh (recommended if the app can stay open a long time)

---

## Freshness / Correctness Rules (important)
Because `collectNow` and `pickupDaySlot` can change with time:
- **Refresh offers on screen enter/resume**
- Also refresh periodically while screen is visible (e.g., every 5–10 minutes)
- Additionally refresh on pull-to-refresh

Do **not** recompute these fields on the device, because correctness must follow vendor timezone (not user timezone).

---

## Kotlin data classes (example)
Parse these fields as strings or proper java.time types.

Example using strings (simpler):
```kotlin
data class OfferDto(
  val id: Long,
  val vendorId: Long?,
  val vendorName: String?,
  val name: String?,
  val description: String?,
  val price: Double,
  val originalPrice: Double,
  val imageUrl: String?,
  val latitude: Double,
  val longitude: Double,

  val pickupDate: String?,        // "2025-12-17"
  val pickupStartTime: String?,   // "19:00:00"
  val pickupEndTime: String?,     // "21:00:00"
  val pickupDaySlot: String?,     // "TODAY" / "TOMORROW" / ...
  val pickupMealSlot: String?,    // "LUNCH" / "DINNER" / ...
  val collectNow: Boolean
)
```

Or parse as `LocalDate` and `LocalTime` using Moshi/Gson adapters.

---

## ViewModel grouping logic (example)
```kotlin
data class HomeSections(
  val collectNow: List<OfferDto>,
  val collectToday: List<OfferDto>,
  val collectTomorrow: List<OfferDto>,
  val lunch: List<OfferDto>,
  val dinner: List<OfferDto>
)

fun buildSections(offers: List<OfferDto>): HomeSections {
  val collectNow = offers.filter { it.collectNow }
  val collectToday = offers.filter { it.pickupDaySlot == "TODAY" && !it.collectNow }
  val collectTomorrow = offers.filter { it.pickupDaySlot == "TOMORROW" }
  val lunch = offers.filter { it.pickupMealSlot == "LUNCH" && (it.pickupDaySlot == "TODAY" || it.pickupDaySlot == "TOMORROW") && !it.collectNow }
  val dinner = offers.filter { it.pickupMealSlot == "DINNER" && (it.pickupDaySlot == "TODAY" || it.pickupDaySlot == "TOMORROW") && !it.collectNow }

  return HomeSections(
    collectNow = collectNow,
    collectToday = collectToday,
    collectTomorrow = collectTomorrow,
    lunch = lunch,
    dinner = dinner
  )
}
```

---

## Offer creation (vendor app) – must send pickupDate
When vendor creates/updates an offer (`POST /vendors/offer-create` and vendor update flow), include:
- `pickupDate` as **ISO `YYYY-MM-DD`** (recommended)
- `pickupStartTime` / `pickupEndTime` as `HH:mm:ss`

Backend also accepts these pickupDate input formats (it will parse and normalize):
- `dd-MM-yyyy` (e.g., `17-12-2025`)
- `dd/MM/yyyy` (e.g., `17/12/2025`)
- `yyyy/MM/dd` (e.g., `2025/12/17`)
- `yyyy.MM.dd` (e.g., `2025.12.17`)

**Canonical output:** the API will return `pickupDate` in ISO `YYYY-MM-DD` regardless of which supported input format was sent.

UI:
- Date picker for pickupDate
- Time pickers for pickupStartTime and pickupEndTime
- Validate end > start

---

## Acceptance Checklist
- [ ] Discover screen shows “Collect now” section when any `collectNow==true`
- [ ] “Collect tomorrow” section shows offers with `pickupDaySlot==TOMORROW`
- [ ] “Collect for lunch/dinner” sections match backend `pickupMealSlot` values
- [ ] “See all” navigates to a full list for that section
- [ ] Screen refreshes periodically so “tomorrow → today” and “collect now” updates correctly


