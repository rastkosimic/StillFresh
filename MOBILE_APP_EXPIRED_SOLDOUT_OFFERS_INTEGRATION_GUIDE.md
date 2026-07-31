# Mobile App AI Agent Prompt: Expired & Sold-Out Offers Display

## Goal
Implement **greyed-out display** for offers that expired or sold out **today**. These offers should still be visible to users (to spark interest in what they missed) but clearly indicated as unavailable. Offers that expired/sold out on previous days are automatically excluded by the backend.

**Important:** The backend automatically filters offers - you will **only receive**:
- Active offers (normal case)
- Offers that expired **today** (in vendor's timezone)
- Offers that sold out **today** (in vendor's timezone)

All other inactive offers are excluded, so you don't need to filter them on the frontend.

---

## New API Response Fields

When you fetch offers (via `/offers/nearby`, `/offers`, etc.), each `OfferDto` now includes these **status flags**:

```json
{
  "id": 123,
  "name": "Fresh Pizza Box",
  "price": 8.50,
  "originalPrice": 15.00,
  "quantityAvailable": 0,
  "active": false,
  
  // ===== NEW STATUS FLAGS =====
  "isExpired": true,        // true if offer expired TODAY (in vendor's timezone)
  "isSoldOut": false,       // true if offer sold out TODAY (in vendor's timezone)
  "isGreyedOut": true,      // true if isExpired || isSoldOut (use this for UI)
  
  // ... other fields (pickupDate, category, etc.)
}
```

### Field Meanings

- **`isExpired`**: `true` if the offer expired **today** (in the vendor's local timezone). `false` otherwise.
- **`isSoldOut`**: `true` if the offer sold out **today** (in the vendor's local timezone). `false` otherwise.
- **`isGreyedOut`**: `true` if either `isExpired` or `isSoldOut` is `true`. **Use this flag to grey out the offer in the UI.**

**Note:** These flags are computed server-side using the vendor's timezone (inferred from coordinates), so you don't need to do any timezone calculations on the client.

---

## API Endpoints (No Changes)

The existing endpoints work the same way, but now include the new flags:

### 1. Nearby Offers
```
GET /offers/nearby?latitude={lat}&longitude={lon}&range={km}&category={optional}
```

### 2. All Offers
```
GET /offers?category={optional}
```

### 3. Vendor Offers
```
GET /offers/{vendorId}/active
GET /offers/{vendorId}/all-offers
```

**All of these endpoints now return offers with `isExpired`, `isSoldOut`, and `isGreyedOut` flags.**

---

## Example API Responses

### Example 1: Active Offer (Normal)
```json
{
  "id": 101,
  "name": "Fresh Bread Basket",
  "price": 5.99,
  "originalPrice": 12.99,
  "quantityAvailable": 10,
  "active": true,
  "isExpired": false,
  "isSoldOut": false,
  "isGreyedOut": false,
  "category": "BREAD_PASTRIES",
  "pickupDate": "2025-12-17",
  "pickupStartTime": "10:00:00",
  "pickupEndTime": "14:00:00"
}
```
**UI Action:** Display normally (no greying).

### Example 2: Expired Today
```json
{
  "id": 102,
  "name": "Lunch Special",
  "price": 9.50,
  "originalPrice": 18.00,
  "quantityAvailable": 3,
  "active": false,
  "isExpired": true,
  "isSoldOut": false,
  "isGreyedOut": true,
  "category": "MEALS",
  "pickupDate": "2025-12-17",
  "pickupStartTime": "12:00:00",
  "pickupEndTime": "14:00:00"
}
```
**UI Action:** Grey out, show "Expired" badge/label.

### Example 3: Sold Out Today
```json
{
  "id": 103,
  "name": "Fresh Croissants",
  "price": 4.99,
  "originalPrice": 8.99,
  "quantityAvailable": 0,
  "active": false,
  "isExpired": false,
  "isSoldOut": true,
  "isGreyedOut": true,
  "category": "BREAD_PASTRIES",
  "pickupDate": "2025-12-17",
  "pickupStartTime": "08:00:00",
  "pickupEndTime": "10:00:00"
}
```
**UI Action:** Grey out, show "Sold Out" badge/label.

---

## Implementation Instructions

### Step 1: Update Data Model

Add the new fields to your `Offer` data class/model:

```kotlin
data class Offer(
    val id: Long,
    val name: String,
    val price: Double,
    val originalPrice: Double,
    val quantityAvailable: Int,
    val active: Boolean,
    
    // NEW FIELDS
    val isExpired: Boolean = false,
    val isSoldOut: Boolean = false,
    val isGreyedOut: Boolean = false,
    
    // ... other existing fields
)
```

### Step 2: Update Offer Card/Item UI

In your offer card/item component (RecyclerView item, Compose card, etc.), apply greying when `isGreyedOut == true`:

#### Option A: Using Compose (Recommended)
```kotlin
@Composable
fun OfferCard(offer: Offer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (offer.isGreyedOut) 0.5f else 1f)  // Grey out effect
    ) {
        Column {
            // Image with overlay
            Box {
                AsyncImage(
                    model = offer.imageUrl,
                    contentDescription = offer.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .graphicsLayer {
                            if (offer.isGreyedOut) {
                                colorFilter = ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(0f) }  // Desaturate
                                )
                            }
                        }
                )
                
                // Status badge overlay
                if (offer.isGreyedOut) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (offer.isExpired) "Expired" else "Sold Out",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Title and price (also greyed)
            Text(
                text = offer.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (offer.isGreyedOut) Color.Gray else Color.Black
            )
            
            // Price with strikethrough if greyed
            Row {
                Text(
                    text = "${offer.price} ${offer.currency}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (offer.isGreyedOut) Color.Gray else Color.Black
                )
                if (offer.isGreyedOut) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
        }
    }
}
```

#### Option B: Using XML/ViewBinding
```kotlin
fun bindOffer(offer: Offer, view: View) {
    val cardView = view.findViewById<CardView>(R.id.offerCard)
    val imageView = view.findViewById<ImageView>(R.id.offerImage)
    val titleText = view.findViewById<TextView>(R.id.offerTitle)
    val priceText = view.findViewById<TextView>(R.id.offerPrice)
    val statusBadge = view.findViewById<TextView>(R.id.statusBadge)
    
    // Apply greying effect
    if (offer.isGreyedOut) {
        cardView.alpha = 0.5f
        imageView.colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply { setToSaturation(0f) }
        )
        titleText.setTextColor(Color.GRAY)
        priceText.setTextColor(Color.GRAY)
        
        // Show status badge
        statusBadge.visibility = View.VISIBLE
        statusBadge.text = if (offer.isExpired) "Expired" else "Sold Out"
        statusBadge.setBackgroundColor(Color.parseColor("#FF6B6B"))
    } else {
        cardView.alpha = 1.0f
        imageView.clearColorFilter()
        titleText.setTextColor(Color.BLACK)
        priceText.setTextColor(Color.BLACK)
        statusBadge.visibility = View.GONE
    }
    
    // ... set other offer data
}
```

### Step 3: Disable Interaction for Greyed-Out Offers

Prevent users from clicking/tapping greyed-out offers:

```kotlin
@Composable
fun OfferCard(offer: Offer, onClick: (Offer) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (offer.isGreyedOut) 0.5f else 1f)
            .clickable(
                enabled = !offer.isGreyedOut,  // Disable clicks if greyed out
                onClick = { onClick(offer) }
            )
    ) {
        // ... card content
    }
}
```

Or in XML:
```xml
<CardView
    android:id="@+id/offerCard"
    android:clickable="@{!offer.isGreyedOut}"
    android:focusable="@{!offer.isGreyedOut}">
    <!-- ... -->
</CardView>
```

### Step 4: Optional - Show Specific Messages

You can use `isExpired` and `isSoldOut` separately for more specific messaging:

```kotlin
val statusMessage = when {
    offer.isExpired -> "This offer expired today"
    offer.isSoldOut -> "This offer sold out today"
    else -> null
}

if (statusMessage != null) {
    Text(
        text = statusMessage,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Red,
        modifier = Modifier.padding(8.dp)
    )
}
```

---

## UI Design Recommendations

### Visual Treatment
1. **Opacity**: Reduce opacity to 0.5-0.6 for greyed-out offers
2. **Image**: Apply grayscale/desaturation filter to images
3. **Text Color**: Use gray color for text instead of black
4. **Badge**: Show a red/orange badge with "Expired" or "Sold Out" text
5. **Strikethrough**: Optionally add strikethrough to price text

### Layout Suggestions
- Place status badge in top-right corner of offer card
- Use a semi-transparent overlay on the image
- Consider adding a subtle border or shadow to indicate unavailability

### Accessibility
- Ensure sufficient contrast even when greyed out
- Add content description: "Expired offer" or "Sold out offer"
- Announce status to screen readers

---

## Testing Scenarios

### Test Case 1: Active Offer
- **Expected**: Normal display, full opacity, clickable
- **Verify**: `isGreyedOut == false`, no greying applied

### Test Case 2: Expired Today
- **Expected**: Greyed out, "Expired" badge visible, not clickable
- **Verify**: `isExpired == true`, `isGreyedOut == true`

### Test Case 3: Sold Out Today
- **Expected**: Greyed out, "Sold Out" badge visible, not clickable
- **Verify**: `isSoldOut == true`, `isGreyedOut == true`

### Test Case 4: Offer Expired Yesterday
- **Expected**: **Not returned by API** (backend filters it out)
- **Verify**: Should not appear in API response at all

---

## Important Notes

1. **No Frontend Filtering Needed**: The backend only returns:
   - Active offers
   - Offers that expired/sold out **today**
   
   You don't need to filter out old expired/sold-out offers - they're already excluded.

2. **Timezone Handling**: All date comparisons are done server-side in the vendor's timezone. You don't need to handle timezones on the client.

3. **Single API Call**: You can get all offers (active + today's expired/sold-out) in one API call. No need for multiple endpoints.

4. **Flags Are Computed Server-Side**: The `isExpired`, `isSoldOut`, and `isGreyedOut` flags are calculated on the backend. Just use them as-is.

5. **Order History**: For viewing past orders, use the existing `/orders` endpoint. This feature is only for showing "missed opportunities" on the Discover/Home screen.

---

## Integration Checklist

- [ ] Update `Offer` data class/model to include `isExpired`, `isSoldOut`, `isGreyedOut` fields
- [ ] Update offer card/item UI to apply greying when `isGreyedOut == true`
- [ ] Add status badge showing "Expired" or "Sold Out" for greyed-out offers
- [ ] Disable click/tap interaction for greyed-out offers
- [ ] Apply visual effects (opacity, grayscale, etc.)
- [ ] Test with active offers (should display normally)
- [ ] Test with expired offers (should be greyed out)
- [ ] Test with sold-out offers (should be greyed out)
- [ ] Verify offers expired/sold out yesterday are not shown (backend filters them)

---

## Example Complete Implementation

```kotlin
// Data Model
data class Offer(
    val id: Long,
    val name: String,
    val price: Double,
    val originalPrice: Double,
    val quantityAvailable: Int,
    val active: Boolean,
    val isExpired: Boolean = false,
    val isSoldOut: Boolean = false,
    val isGreyedOut: Boolean = false,
    val imageUrl: String?,
    val category: String?,
    // ... other fields
)

// Composable UI
@Composable
fun OfferCard(
    offer: Offer,
    onClick: (Offer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .alpha(if (offer.isGreyedOut) 0.5f else 1f)
            .clickable(
                enabled = !offer.isGreyedOut,
                onClick = { onClick(offer) }
            )
    ) {
        Column {
            // Image with overlay
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = offer.imageUrl,
                    contentDescription = offer.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .graphicsLayer {
                            if (offer.isGreyedOut) {
                                colorFilter = ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(0f) }
                                )
                            }
                        },
                    contentScale = ContentScale.Crop
                )
                
                // Status badge
                if (offer.isGreyedOut) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (offer.isExpired) "Expired" else "Sold Out",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Content
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = offer.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (offer.isGreyedOut) Color.Gray else Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${offer.price} ${offer.currency}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (offer.isGreyedOut) Color.Gray else Color.Black
                    )
                    if (offer.isGreyedOut) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}
```

---

## Summary

1. **Backend filters automatically** - only active + today's expired/sold-out offers are returned
2. **Use `isGreyedOut` flag** to grey out offers in the UI
3. **Disable interaction** for greyed-out offers (no clicks/taps)
4. **Show status badge** with "Expired" or "Sold Out" text
5. **Apply visual effects** (opacity, grayscale) for clear indication
6. **No timezone handling needed** - all done server-side

The goal is to show users what they missed today, sparking interest while clearly indicating unavailability.

