# Mobile App AI Agent Prompt: Category Integration

## Overview
Your Android app needs to integrate the category system for offers. Categories support multiple languages (Serbian, Croatian, Montenegrin, Bosnian, Slovenian, Bulgarian, Romanian, Macedonian) with automatic fallback to English.

## API Endpoints

### 1. Get Categories (with Localization)
```
GET /offers/categories?locale={locale}
```

**Parameters:**
- `locale` (optional, default: "en"): Language code (e.g., "sr", "hr", "bg", "ro", "mk", "sl", "bs", "me")

**Response:**
```json
[
  { "value": "MEALS", "displayName": "Obroci" },
  { "value": "BREAD_PASTRIES", "displayName": "Hleb & Peciva" },
  { "value": "GROCERIES", "displayName": "Namirnice" },
  { "value": "FLOWERS_PLANTS", "displayName": "Cveće & Biljke" },
  { "value": "PET_FOOD", "displayName": "Hrana za kućne ljubimce" }
]
```

**Example:**
- English: `GET /offers/categories?locale=en` → "Meals", "Bread & pastries", etc.
- Serbian: `GET /offers/categories?locale=sr` → "Obroci", "Hleb & Peciva", etc.
- Bulgarian: `GET /offers/categories?locale=bg` → "Ястия", "Хляб & Печива", etc.

### 2. Get Offers (with Category Filter)
```
GET /offers?category={category}
```

**Parameters:**
- `category` (optional): Category enum value (e.g., "MEALS", "GROCERIES", "BREAD_PASTRIES")

**Response:** List of `OfferDto` objects (same as before, now includes `category` field)

### 3. Get Nearby Offers (with Category Filter)
```
GET /offers/nearby?latitude={lat}&longitude={lon}&range={range}&category={category}
```

**Parameters:**
- `latitude`, `longitude`, `range`: Same as before
- `category` (optional): Category enum value to filter by

### 4. Create Offer (with Category)
```
POST /vendors/offer-create
```

**Request Body:**
```json
{
  "name": "Fresh Salad Box",
  "description": "Mixed greens",
  "price": 5.99,
  "category": "MEALS",  // Optional - will map from businessType if not provided
  ...
}
```

## Implementation Steps

### Step 1: Create Category Model

```kotlin
data class Category(
    val value: String,        // "MEALS", "GROCERIES", etc.
    val displayName: String   // "Obroci", "Namirnice", etc. (localized)
)

enum class OfferCategory {
    ALL,
    MEALS,
    BREAD_PASTRIES,
    GROCERIES,
    FLOWERS_PLANTS,
    PET_FOOD
}
```

### Step 2: Get User's Locale

```kotlin
fun getUserLocale(): String {
    val locale = Locale.getDefault()
    val language = locale.language.lowercase()
    
    // Map Android locale to supported backend locales
    return when (language) {
        "sr" -> "sr"  // Serbian
        "hr" -> "hr"  // Croatian
        "bs" -> "bs"  // Bosnian
        "sl" -> "sl"  // Slovenian
        "bg" -> "bg"  // Bulgarian
        "ro" -> "ro"  // Romanian
        "mk" -> "mk"  // Macedonian
        else -> "en"  // Default to English
    }
}
```

### Step 3: Fetch Categories from API

```kotlin
suspend fun fetchCategories(locale: String = getUserLocale()): List<Category> {
    val response = apiService.getCategories(locale)
    return response.body() ?: emptyList()
}

// API Service
@GET("offers/categories")
suspend fun getCategories(
    @Query("locale") locale: String = "en"
): Response<List<Category>>
```

### Step 4: Update Offer Model

```kotlin
data class OfferDto(
    val id: Long,
    val name: String,
    val price: Double,
    val category: String?,  // "MEALS", "GROCERIES", etc. (nullable for backward compatibility)
    // ... other fields
)
```

### Step 5: Display Categories in UI

#### Category Filter Bar (Horizontal Scrollable)
```kotlin
@Composable
fun CategoryFilterBar(
    categories: List<Category>,
    selectedCategory: OfferCategory?,
    onCategorySelected: (OfferCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // "All" option
        item {
            CategoryChip(
                label = "All",  // Or translate based on locale
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        
        // Category options
        items(categories) { category ->
            CategoryChip(
                label = category.displayName,
                isSelected = selectedCategory?.name == category.value,
                onClick = { 
                    onCategorySelected(OfferCategory.valueOf(category.value))
                }
            )
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4CAF50),  // Green when selected
            selectedLabelColor = Color.White
        )
    )
}
```

### Step 6: Filter Offers by Category

```kotlin
// In your ViewModel or Repository
suspend fun getOffers(category: OfferCategory? = null): List<OfferDto> {
    val categoryParam = category?.name
    return apiService.getOffers(category = categoryParam)
}

suspend fun getNearbyOffers(
    latitude: Double,
    longitude: Double,
    range: Double,
    category: OfferCategory? = null
): List<OfferDto> {
    val categoryParam = category?.name
    return apiService.getNearbyOffers(
        latitude = latitude,
        longitude = longitude,
        range = range,
        category = categoryParam
    )
}

// API Service
@GET("offers")
suspend fun getOffers(
    @Query("category") category: String? = null
): Response<List<OfferDto>>

@GET("offers/nearby")
suspend fun getNearbyOffers(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("range") range: Double,
    @Query("category") category: String? = null
): Response<List<OfferDto>>
```

### Step 7: Include Category in Offer Creation

```kotlin
// In offer creation form
data class CreateOfferRequest(
    val name: String,
    val description: String,
    val price: Double,
    val category: String?,  // Optional - backend will map from businessType if null
    // ... other fields
)

// When user selects category in form
var selectedCategory by remember { mutableStateOf<OfferCategory?>(null) }

// In category picker
CategoryPicker(
    categories = categories,
    selectedCategory = selectedCategory,
    onCategorySelected = { selectedCategory = it }
)

// When submitting offer
val request = CreateOfferRequest(
    name = offerName,
    description = offerDescription,
    price = offerPrice,
    category = selectedCategory?.name,  // Send enum name: "MEALS", "GROCERIES", etc.
    // ... other fields
)

apiService.createOffer(request)
```

### Step 8: Display Category in Offer Cards

```kotlin
@Composable
fun OfferCard(offer: OfferDto, locale: String = getUserLocale()) {
    Card {
        Column {
            // Offer image
            AsyncImage(model = offer.imageUrl, ...)
            
            // Category badge
            offer.category?.let { categoryValue ->
                CategoryBadge(
                    category = OfferCategory.valueOf(categoryValue),
                    locale = locale
                )
            }
            
            // Offer details
            Text(offer.name)
            Text("€${offer.price}")
            // ... other fields
        }
    }
}

@Composable
fun CategoryBadge(
    category: OfferCategory,
    locale: String
) {
    // Fetch display name from your categories list or translate locally
    val displayName = getCategoryDisplayName(category, locale)
    
    Surface(
        color = Color(0xFFE8F5E9),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
```

## Complete Example: Category Filter Screen

```kotlin
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val locale = getUserLocale()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val offers by viewModel.offers.collectAsState()
    
    LaunchedEffect(locale) {
        viewModel.loadCategories(locale)
    }
    
    LaunchedEffect(selectedCategory) {
        viewModel.loadOffers(selectedCategory)
    }
    
    Column {
        // Category filter bar
        CategoryFilterBar(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) }
        )
        
        // Offers list
        LazyColumn {
            items(offers) { offer ->
                OfferCard(offer = offer, locale = locale)
            }
        }
    }
}

class DiscoverViewModel @Inject constructor(
    private val offerRepository: OfferRepository
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<OfferCategory?>(null)
    val selectedCategory: StateFlow<OfferCategory?> = _selectedCategory.asStateFlow()
    
    private val _offers = MutableStateFlow<List<OfferDto>>(emptyList())
    val offers: StateFlow<List<OfferDto>> = _offers.asStateFlow()
    
    fun loadCategories(locale: String) {
        viewModelScope.launch {
            try {
                val cats = offerRepository.fetchCategories(locale)
                _categories.value = cats
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun selectCategory(category: OfferCategory?) {
        _selectedCategory.value = category
    }
    
    fun loadOffers(category: OfferCategory?) {
        viewModelScope.launch {
            try {
                val offersList = offerRepository.getOffers(category)
                _offers.value = offersList
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
```

## Best Practices

### 1. Cache Categories
```kotlin
// Cache categories locally to avoid repeated API calls
class CategoryRepository {
    private var cachedCategories: List<Category>? = null
    private var cachedLocale: String? = null
    
    suspend fun getCategories(locale: String): List<Category> {
        if (cachedCategories != null && cachedLocale == locale) {
            return cachedCategories!!
        }
        
        val categories = apiService.getCategories(locale)
        cachedCategories = categories
        cachedLocale = locale
        return categories
    }
}
```

### 2. Handle Locale Changes
```kotlin
// Reload categories when user changes language
LaunchedEffect(userLocale) {
    viewModel.loadCategories(userLocale)
}
```

### 3. Error Handling
```kotlin
// If category fetch fails, use default English categories
suspend fun fetchCategories(locale: String): List<Category> {
    return try {
        apiService.getCategories(locale)
    } catch (e: Exception) {
        // Fallback to English
        if (locale != "en") {
            apiService.getCategories("en")
        } else {
            emptyList()
        }
    }
}
```

### 4. Category Validation
```kotlin
// Validate category enum value before sending to API
fun isValidCategory(category: String?): Boolean {
    if (category == null) return true  // Optional field
    
    return try {
        OfferCategory.valueOf(category.uppercase())
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}
```

## UI/UX Recommendations

1. **Category Filter Bar:**
   - Place at top of discover/browse screen
   - Horizontal scrollable
   - "All" option should be first
   - Selected category highlighted in green
   - Smooth scrolling to selected category

2. **Offer Cards:**
   - Show category as small badge/chip
   - Use subtle color (light green background)
   - Position near offer title or image

3. **Category Selection:**
   - In offer creation form, use dropdown or chips
   - Make it optional (backend handles fallback)
   - Show category preview before submission

4. **Loading States:**
   - Show skeleton loaders while fetching categories
   - Show loading indicator when filtering offers

## Testing Checklist

- [ ] Categories load correctly for each supported language
- [ ] Category filter works (shows only offers in selected category)
- [ ] "All" option shows all offers
- [ ] Category appears in offer cards
- [ ] Category can be selected when creating offer
- [ ] Offer creation works without category (fallback to businessType)
- [ ] Locale detection works correctly
- [ ] Fallback to English works for unsupported locales
- [ ] Nearby offers filtering by category works
- [ ] Category filter persists when navigating between screens

## API Base URL

Use your API Gateway URL:
- Development: `http://localhost:8080`
- Production: Your production API Gateway URL

## Summary

1. ✅ Fetch categories from `/offers/categories?locale={locale}`
2. ✅ Display categories in horizontal filter bar
3. ✅ Filter offers by category using `?category={category}` parameter
4. ✅ Include category (optional) when creating offers
5. ✅ Display category badge on offer cards
6. ✅ Handle locale detection and fallback to English
7. ✅ Cache categories to reduce API calls

The backend handles all validation and fallback logic, so the app just needs to:
- Fetch and display categories
- Send category enum value (e.g., "MEALS") when filtering/creating
- Handle the localized display names from the API

