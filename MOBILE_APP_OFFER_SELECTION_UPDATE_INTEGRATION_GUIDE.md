# Mobile App AI Agent Prompt: Offer Selection and Update (Reset) Functionality

## Goal
Implement functionality for vendors to **select and update/reset their offers**, including expired and sold-out ones. This enables vendors to quickly reactivate offers with new dates/times without recreating them from scratch.

**Key Use Case:** Vendors often have the same offers day-to-day. Instead of creating new offers every day, they can select existing offers (even expired ones) and update them with new pickup dates/times to reactivate them.

---

## API Endpoints

### 1. Get All Offers (Including Expired/Sold-Out)
```
GET /vendors/all-offers
```

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response (200 OK):**
```json
[
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
    "pickupDate": "2025-12-22",
    "pickupStartTime": "10:00:00",
    "pickupEndTime": "14:00:00",
    "category": "BREAD_PASTRIES",
    "imageUrl": "https://example.com/bread.jpg"
  },
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
    "pickupDate": "2025-12-20",
    "pickupStartTime": "12:00:00",
    "pickupEndTime": "14:00:00",
    "category": "MEALS",
    "imageUrl": "https://example.com/lunch.jpg"
  },
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
    "pickupDate": "2025-12-21",
    "pickupStartTime": "08:00:00",
    "pickupEndTime": "10:00:00",
    "category": "BREAD_PASTRIES",
    "imageUrl": "https://example.com/croissants.jpg"
  }
]
```

**Note:** This endpoint returns **ALL offers** for the authenticated vendor, including:
- Active offers
- Expired offers (any date, not just today)
- Sold-out offers (any date, not just today)

### 2. Update/Reset Offer
```
POST /vendors/update-offer/{offerId}
```

**Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Path Parameters:**
- `offerId` (Long): ID of the offer to update

**Request Body:**
```json
{
  "name": "Fresh Bread Basket",
  "description": "Assorted fresh breads",
  "price": 5.99,
  "originalPrice": 12.99,
  "quantityAvailable": 10,
  "pickupDate": "2025-12-23",
  "pickupStartTime": "10:00:00",
  "pickupEndTime": "14:00:00",
  "category": "BREAD_PASTRIES",
  "dietaryInfo": "Vegetarian",
  "allergenInfo": "Contains gluten",
  "imageUrl": "https://example.com/bread.jpg"
}
```

**Important Fields for Reset:**
- `pickupDate` (string, `yyyy-MM-dd`): New pickup date - **required for reset**
- `pickupStartTime` (string, `HH:mm:ss`): New start time - **required for reset**
- `pickupEndTime` (string, `HH:mm:ss`): New end time - **required for reset**
- `quantityAvailable` (int): Reset quantity if offer was sold out
- Other fields: Update as needed, or keep existing values

**Response (200 OK):**
```
"Offer updated successfully"
```

**What Happens on Update:**
1. Offer is **reactivated** (`active = true`)
2. **Expired/sold-out status is cleared** (`expiredAt = null`, `soldOutAt = null`)
3. **Expiration date is automatically recalculated** from `pickupDate + pickupEndTime` in vendor's timezone
4. All other fields are updated as provided

**Error Responses:**

**400 Bad Request** (Validation Error):
```json
{
  "error": "Cannot create offer: Pickup end time (2025-12-20 12:00) must be in the future. Current time in vendor timezone: 2025-12-20 21:45:56"
}
```

**Common Error Messages:**
- `"Cannot create offer: Pickup end time (date time) must be in the future. Current time in vendor timezone: (date time)"`
- `"Cannot update offer: Pickup end time (date time) must be in the future. Current time in vendor timezone: (date time)"`

**Other Error Responses:**
- **401 Unauthorized**: Token expired - redirect to login
- **404 Not Found**: Offer not found or doesn't belong to vendor
- **500 Internal Server Error**: Server-side error

---

## Implementation Instructions

### Step 1: Data Model

Update your `Offer` data class to include status fields:

```kotlin
data class Offer(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Double,
    val originalPrice: Double,
    val quantityAvailable: Int,
    val active: Boolean,
    
    // Status flags
    val isExpired: Boolean = false,
    val isSoldOut: Boolean = false,
    val isGreyedOut: Boolean = false,
    
    // Pickup information
    val pickupDate: String?,  // yyyy-MM-dd format
    val pickupStartTime: String?,  // HH:mm:ss format
    val pickupEndTime: String?,  // HH:mm:ss format
    
    // Other fields
    val category: String?,
    val imageUrl: String?,
    val dietaryInfo: String?,
    val allergenInfo: String?,
    // ... other existing fields
)
```

### Step 2: Fetch All Offers Screen

Create a screen to display all offers (vendor's offer management screen):

```kotlin
@Composable
fun VendorOffersScreen(
    onOfferSelected: (Offer) -> Unit,
    modifier: Modifier = Modifier
) {
    var offers by remember { mutableStateOf<List<Offer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            offers = fetchAllOffers()
            error = null
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Text(
            text = "My Offers",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            error != null -> ErrorMessage(error!!)
            offers.isEmpty() -> EmptyState("No offers found")
            else -> OfferList(
                offers = offers,
                onOfferClick = onOfferSelected
            )
        }
    }
}

suspend fun fetchAllOffers(): List<Offer> {
    val response = httpClient.get("${API_BASE_URL}/vendors/all-offers") {
        headers {
            append("Authorization", "Bearer ${getAuthToken()}")
        }
    }
    
    if (!response.status.isSuccess()) {
        throw Exception("Failed to fetch offers: ${response.status}")
    }
    
    return response.body<List<Offer>>()
}
```

### Step 3: Offer List with Status Indicators

Display offers with clear status indicators:

```kotlin
@Composable
fun OfferList(
    offers: List<Offer>,
    onOfferClick: (Offer) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(offers) { offer ->
            OfferListItem(
                offer = offer,
                onClick = { onOfferClick(offer) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun OfferListItem(
    offer: Offer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            AsyncImage(
                model = offer.imageUrl,
                contentDescription = offer.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Name with status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = offer.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Status badge
                    if (offer.isGreyedOut) {
                        StatusBadge(
                            text = if (offer.isExpired) "Expired" else "Sold Out",
                            color = Color.Red
                        )
                    } else if (offer.active) {
                        StatusBadge(
                            text = "Active",
                            color = Color.Green
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Price
                Text(
                    text = "${offer.price} ${offer.currency}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Pickup info
                if (offer.pickupDate != null && offer.pickupEndTime != null) {
                    Text(
                        text = "Pickup: ${formatDate(offer.pickupDate)} ${formatTime(offer.pickupEndTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Quantity
                Text(
                    text = "Available: ${offer.quantityAvailable}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
```

### Step 4: Offer Update/Reset Screen

Create a screen to update/reset an offer:

```kotlin
@Composable
fun UpdateOfferScreen(
    offer: Offer,
    onUpdateSuccess: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pickupDate by remember { mutableStateOf(offer.pickupDate ?: "") }
    var pickupStartTime by remember { mutableStateOf(offer.pickupStartTime ?: "") }
    var pickupEndTime by remember { mutableStateOf(offer.pickupEndTime ?: "") }
    var quantityAvailable by remember { mutableStateOf(offer.quantityAvailable.toString()) }
    var isUpdating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Update Offer: ${offer.name}",
            style = MaterialTheme.typography.headlineSmall
        )
        
        if (offer.isGreyedOut) {
            // Show reset message for expired/sold-out offers
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = if (offer.isExpired) 
                        "This offer has expired. Update the dates to reactivate it."
                    else 
                        "This offer is sold out. Update the dates and quantity to reactivate it.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Pickup Date
        OutlinedTextField(
            value = pickupDate,
            onValueChange = { pickupDate = it },
            label = { Text("Pickup Date (yyyy-MM-dd)") },
            placeholder = { Text("2025-12-23") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Pickup Start Time
        OutlinedTextField(
            value = pickupStartTime,
            onValueChange = { pickupStartTime = it },
            label = { Text("Start Time (HH:mm:ss)") },
            placeholder = { Text("10:00:00") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Pickup End Time
        OutlinedTextField(
            value = pickupEndTime,
            onValueChange = { pickupEndTime = it },
            label = { Text("End Time (HH:mm:ss)") },
            placeholder = { Text("14:00:00") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Quantity (especially important for sold-out offers)
        OutlinedTextField(
            value = quantityAvailable,
            onValueChange = { quantityAvailable = it },
            label = { Text("Quantity Available") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        // Error message
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    isUpdating = true
                    error = null
                    
                    // Update offer
                    coroutineScope.launch {
                        try {
                            updateOffer(
                                offerId = offer.id,
                                pickupDate = pickupDate,
                                pickupStartTime = pickupStartTime,
                                pickupEndTime = pickupEndTime,
                                quantityAvailable = quantityAvailable.toIntOrNull() ?: offer.quantityAvailable,
                                // Include other fields from original offer
                                name = offer.name,
                                description = offer.description,
                                price = offer.price,
                                originalPrice = offer.originalPrice,
                                category = offer.category,
                                imageUrl = offer.imageUrl,
                                dietaryInfo = offer.dietaryInfo,
                                allergenInfo = offer.allergenInfo
                            )
                            onUpdateSuccess()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to update offer"
                        } finally {
                            isUpdating = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (offer.isGreyedOut) "Reset Offer" else "Update Offer")
                }
            }
        }
    }
}

suspend fun updateOffer(
    offerId: Long,
    pickupDate: String,
    pickupStartTime: String,
    pickupEndTime: String,
    quantityAvailable: Int,
    name: String,
    description: String?,
    price: Double,
    originalPrice: Double,
    category: String?,
    imageUrl: String?,
    dietaryInfo: String?,
    allergenInfo: String?
) {
    val requestBody = buildJsonObject {
        put("name", name)
        put("description", description ?: "")
        put("price", price)
        put("originalPrice", originalPrice)
        put("quantityAvailable", quantityAvailable)
        put("pickupDate", pickupDate)
        put("pickupStartTime", pickupStartTime)
        put("pickupEndTime", pickupEndTime)
        category?.let { put("category", it) }
        imageUrl?.let { put("imageUrl", it) }
        dietaryInfo?.let { put("dietaryInfo", it) }
        allergenInfo?.let { put("allergenInfo", it) }
    }
    
    val response = httpClient.post("${API_BASE_URL}/vendors/update-offer/$offerId") {
        headers {
            append("Authorization", "Bearer ${getAuthToken()}")
            append("Content-Type", "application/json")
        }
        setBody(requestBody.toString())
    }
    
    if (!response.status.isSuccess()) {
        val errorBody = response.bodyAsText()
        throw Exception("Failed to update offer: ${response.status} - $errorBody")
    }
}
```

### Step 5: Navigation Flow

```kotlin
// In your navigation setup
NavHost(
    navController = navController,
    startDestination = "vendor_offers"
) {
    composable("vendor_offers") {
        VendorOffersScreen(
            onOfferSelected = { offer ->
                navController.navigate("update_offer/${offer.id}")
            }
        )
    }
    
    composable("update_offer/{offerId}") { backStackEntry ->
        val offerId = backStackEntry.arguments?.getString("offerId")?.toLongOrNull()
        val offer = remember { /* Get offer from state or fetch */ }
        
        UpdateOfferScreen(
            offer = offer,
            onUpdateSuccess = {
                navController.popBackStack()
                // Show success message
            },
            onCancel = {
                navController.popBackStack()
            }
        )
    }
}
```

---

## UI/UX Recommendations

### 1. Offer List Display
- **Group offers** by status (Active, Expired, Sold Out) with section headers
- **Visual indicators**: Use color-coded badges or icons
  - Green badge: Active
  - Red badge: Expired/Sold Out
- **Show pickup date/time** prominently for quick reference
- **Enable selection**: Make entire card clickable

### 2. Update Screen
- **Pre-fill fields** with current offer values
- **Highlight required fields** for reset (pickupDate, pickupStartTime, pickupEndTime)
- **Show helpful message** for expired/sold-out offers explaining they'll be reactivated
- **Date/Time pickers**: Consider using native date/time pickers instead of text input
- **Validation**: Validate date format (yyyy-MM-dd) and time format (HH:mm:ss)

### 3. Success Feedback
- Show **snackbar/toast** on successful update: "Offer updated and reactivated successfully"
- **Refresh offer list** after update
- **Navigate back** to offer list

### 4. Error Handling
- **Network errors**: Show retry option
- **Validation errors**: Highlight invalid fields
- **Permission errors**: Redirect to login if token expired

---

## Date/Time Format Helpers

```kotlin
fun formatDate(dateString: String?): String {
    if (dateString == null) return ""
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

fun formatTime(timeString: String?): String {
    if (timeString == null) return ""
    return try {
        val time = LocalTime.parse(timeString)
        time.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        timeString
    }
}
```

---

## Testing Scenarios

### Test Case 1: Update Active Offer
- **Action**: Select active offer, update pickup date/time
- **Expected**: Offer updated with new dates, remains active

### Test Case 2: Reset Expired Offer
- **Action**: Select expired offer, update with new dates
- **Expected**: Offer reactivated, `isExpired = false`, new expiration date calculated

### Test Case 3: Reset Sold-Out Offer
- **Action**: Select sold-out offer, update quantity and dates
- **Expected**: Offer reactivated, `isSoldOut = false`, quantity reset

### Test Case 4: Validation
- **Action**: Submit update with invalid date format
- **Expected**: Error message shown, offer not updated

### Test Case 5: Network Error
- **Action**: Update offer with no network
- **Expected**: Error message with retry option

---

## Summary

1. **Fetch all offers** using `GET /vendors/all-offers` (includes expired/sold-out)
2. **Display offers** with status indicators (Active/Expired/Sold Out)
3. **Select offer** to update/reset
4. **Update offer** using `POST /vendors/update-offer/{offerId}` with new dates/times
5. **Backend automatically**:
   - Reactivates offer (`active = true`)
   - Clears expired/sold-out status
   - Recalculates expiration date from new pickup date/time
6. **Show success feedback** and refresh list
7. **Handle validation errors** (400 Bad Request) when pickup time is in the past

## Important: Error Handling

**Always check for validation errors** when creating or updating offers:

- **400 Bad Request**: Pickup date/time validation failed
  - Error message contains: `"must be in the future"`
  - Show user-friendly message: "Pickup time must be in the future"
  - Highlight date/time input fields
  - Suggest selecting a future date/time

- **200 OK**: Offer created/updated successfully
- **401 Unauthorized**: Token expired - redirect to login
- **500 Internal Server Error**: Server error - show generic error message

**Example Error Handling Pattern:**
```kotlin
try {
    val response = createOffer(offer)
    if (response.status.isSuccess()) {
        // Success
    } else if (response.status.value == 400) {
        // Validation error - parse and display
        val error = response.body<Map<String, String>>()
        showValidationError(error["error"])
    }
} catch (e: Exception) {
    // Network or other error
    showError(e.message)
}
```

This enables vendors to quickly reset their daily offers without recreating them from scratch, with proper validation and error feedback!

