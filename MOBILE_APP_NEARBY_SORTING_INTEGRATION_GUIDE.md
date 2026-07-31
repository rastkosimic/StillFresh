## ANDROID MOBILE APP – NEARBY OFFERS SORTING INTEGRATION GUIDE

This guide explains how the Android app should call the **Nearby Offers** endpoint and how to implement **sorting** by:

- **Distance** (default)
- **Price (Low → High)**
- **Price (High → Low)**
- **Rating (High → Low)**

The goal is to have a single, flexible endpoint that can serve all nearby-offer use cases while keeping the mobile implementation simple and predictable.

---

## 1. Backend API Overview

### 1.1 Base Endpoint

- **Path**: `/offers/nearby`
- **Method**: `GET`
- **Description**: Returns offers around the given coordinates, filtered by range and category, and sorted according to the `sort` query parameter.

### 1.2 Query Parameters

- **`latitude`** (required, `double`)
  - User’s latitude.
- **`longitude`** (required, `double`)
  - User’s longitude.
- **`range`** (optional, `double`, default: `10`)
  - Maximum distance in **kilometers** from the user.
- **`category`** (optional, `string`)
  - Offer category, e.g. `"MEALS"`, `"GROCERIES"`, `"BREAD_PASTRIES"`, etc.
  - If omitted, all categories are considered.
- **`sort`** (optional, `string`, default: `"distance"`)
  - Sorting strategy for the returned offers.

### 1.3 Supported `sort` Values

- **`distance`** *(default)*
  - Offers are sorted from **nearest to farthest**.
- **`price_asc`**
  - Offers are sorted by **price from lowest to highest**.
- **`price_desc`**
  - Offers are sorted by **price from highest to lowest**.
- **`rating_desc`**
  - Offers are sorted by **average rating from highest to lowest**.

> If `sort` is missing or invalid, the backend falls back to **distance** sorting.

### 1.4 Example Requests

#### 1.4.1 Default (Distance) Sorting

```text
GET /offers/nearby?latitude=49.3733&longitude=8.6833&range=10
```

#### 1.4.2 Distance Sorting With Category Filter

```text
GET /offers/nearby?latitude=49.3733&longitude=8.6833&range=10&category=MEALS&sort=distance
```

#### 1.4.3 Price – Low to High

```text
GET /offers/nearby?latitude=49.3733&longitude=8.6833&range=10&sort=price_asc
```

#### 1.4.4 Price – High to Low

```text
GET /offers/nearby?latitude=49.3733&longitude=8.6833&range=10&sort=price_desc
```

#### 1.4.5 Rating – High to Low

```text
GET /offers/nearby?latitude=49.3733&longitude=8.6833&range=10&sort=rating_desc
```

---

## 2. Response Format

The response is a JSON array of `OfferDto` objects. The **structure is identical** regardless of the sorting mode; only the **order of elements** changes.

**Example (simplified):**

```json
[
  {
    "id": 18,
    "name": "Evening Offer",
    "description": "Test offer",
    "price": 3.0,
    "originalPrice": 6.0,
    "quantityAvailable": 8,
    "pickupDate": "2025-12-21",
    "pickupStartTime": "11:00:00",
    "pickupEndTime": "12:00:00",
    "category": "MEALS",
    "rating": 4.7,
    "reviewsCount": 123,
    "address": "33 Kolbenzeil, Heidelberg, GERMANY",
    "zipCode": "69126",
    "latitude": 49.3733326,
    "longitude": 8.6833014,
    "imageUrl": "https://example.com/image.jpg",
    "currency": "EUR",
    "businessType": "Restaurant",
    "pickupDaySlot": "TODAY",
    "pickupMealSlot": "DINNER",
    "collectNow": false,
    "isExpired": false,
    "isSoldOut": false,
    "isGreyedOut": false
  },
  {
    "id": 16,
    "name": "Morning Offer",
    "price": 2.5,
    "...": "..."
  }
]
```

> The **first element** in the array is always the **highest priority item** according to the selected sort: nearest, cheapest, or highest rated.

---

## 3. Android Integration – High Level

### 3.1 Sort Options in the UI

Expose a sort selector in the Nearby Offers screen, e.g. using **chips**, **tabs**, or a **dropdown**:

- **Distance (default)**
- **Price: Low to High**
- **Price: High to Low**
- **Rating: High to Low**

Map these UI options to API values:

| UI Label              | `sort` value  |
|-----------------------|--------------:|
| Distance (default)    | `"distance"`  |
| Price: Low to High    | `"price_asc"` |
| Price: High to Low    | `"price_desc"`|
| Rating: High to Low   | `"rating_desc"` |

### 3.2 Recommended Data Model

Use the same `Offer` data class you already use for displaying offers in the app. No new fields are required for sorting.

```kotlin
data class Offer(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Double,
    val originalPrice: Double?,
    val quantityAvailable: Int,
    val pickupDate: String?,
    val pickupStartTime: String?,
    val pickupEndTime: String?,
    val category: String?,
    val rating: Double?,
    val reviewsCount: Int?,
    val address: String?,
    val zipCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val currency: String?,
    val businessType: String?,
    val pickupDaySlot: String?,
    val pickupMealSlot: String?,
    val collectNow: Boolean,
    val isExpired: Boolean,
    val isSoldOut: Boolean,
    val isGreyedOut: Boolean
)
```

---

## 4. Android – API Client Implementation

### 4.1 Sort Enum (Recommended)

```kotlin
enum class NearbySort(val apiValue: String) {
    DISTANCE("distance"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc"),
    RATING_DESC("rating_desc");
}
```

### 4.2 API Call Function

```kotlin
suspend fun fetchNearbyOffers(
    latitude: Double,
    longitude: Double,
    rangeKm: Double = 10.0,
    sort: NearbySort = NearbySort.DISTANCE,
    category: String? = null
): List<Offer> {
    val queryParams = mutableListOf(
        "latitude=${latitude}",
        "longitude=${longitude}",
        "range=${rangeKm}",
        "sort=${sort.apiValue}"
    )

    if (!category.isNullOrBlank()) {
        queryParams += "category=${category}"
    }

    val url = "/offers/nearby?${queryParams.joinToString("&")}"

    val response = apiClient.get(url)
    // Deserialize into List<Offer>
    return json.decodeFromString(response.bodyAsText())
}
```

> The only difference between the sort modes is the **`sort` query parameter** included in the request URL.

---

## 5. UI Behavior and State Management

### 5.1 Default Load

1. On Nearby screen open:
   - Detect or receive user location (latitude & longitude).
   - Set `sort = NearbySort.DISTANCE`.
2. Call `fetchNearbyOffers(...)` with default sort.
3. Display the returned list in the order received.

### 5.2 Changing Sort Option

When the user selects a different sort mode:

1. Update local state: `selectedSort = newSort`.
2. Re-call the API with the new `sort`:
   - **Do not** sort again on the client side; trust the server ordering.
3. Replace the list with the new response.
4. Optionally, show a small loading indicator during the refresh.

### 5.3 Combining With Category Filter

If you also support categories (e.g., MEALS, GROCERIES, etc.):

- Keep both **category** and **sort** in state:
  - `selectedCategory`
  - `selectedSort`
- Every time either changes, re-fetch nearby offers with both query parameters.

Example:

```kotlin
val offers = fetchNearbyOffers(
    latitude = userLat,
    longitude = userLon,
    rangeKm = 10.0,
    sort = selectedSort,
    category = selectedCategory?.name
)
```

---

## 6. Error Handling

### 6.1 Network or Server Errors

If the request fails due to connectivity or server issues:

- Show a **toast/snackbar** with a generic error message:
  - “Unable to load nearby offers. Please check your connection and try again.”
- Optionally show a **“Retry”** button.

### 6.2 Invalid Sort Values (Defensive)

If the app somehow sends an invalid `sort` (e.g., due to a bug or old client):

- The backend falls back to **distance** sorting.
- From the app’s perspective, it will just see a valid list sorted by distance.

### 6.3 Empty Results

If the response is an empty list:

- Display a friendly empty state:
  - “No nearby offers found for this area.”
  - Suggest increasing range or changing filters.

---

## 7. Testing Checklist

### 7.1 Distance Sorting

- **Given** multiple offers at different distances
- **When** calling `/offers/nearby?sort=distance`
- **Then** offers are ordered from **nearest to farthest**.

### 7.2 Price Ascending

- **When** calling `/offers/nearby?sort=price_asc`
- **Then** the **cheapest** offer appears first, and prices are non-decreasing.

### 7.3 Price Descending

- **When** calling `/offers/nearby?sort=price_desc`
- **Then** the **most expensive** offer appears first, and prices are non-increasing.

### 7.4 Rating Descending

- **When** calling `/offers/nearby?sort=rating_desc`
- **Then** offers with higher `rating` appear before those with lower ratings.

### 7.5 Category + Sorting

- **When** using `category` + any `sort` value together
- **Then** only offers matching the category are returned, **and** they are ordered by the chosen sort.

### 7.6 Default Behavior

- **When** `sort` is not provided
- **Then** the endpoint behaves exactly as `sort=distance`.

---

## 8. Summary

1. Use **one endpoint**: `GET /offers/nearby`.
2. Control ordering via the **`sort` query parameter**:
   - `distance` (default), `price_asc`, `price_desc`, `rating_desc`.
3. The **response structure is unchanged**; only ordering differs.
4. Android app:
   - Exposes sort options in the UI.
   - Sends `sort` to the backend.
   - Displays offers in the order returned by the API (no extra client-side sorting needed).

This design keeps both the backend and mobile app simple while providing a flexible, future‑proof way to present nearby offers according to the user’s preferred sorting mode.


