# Category Implementation Analysis

## Current State Analysis

### From Screenshots:
The mobile app displays categories like:
- **Top-level filters:** "All", "Meals", "Bread & pastries", "Groceries", "Flowers & plants", "Pet food"
- **Time filters:** "Collect today", "Collect tomorrow"
- **Dietary filters:** "Vegetarian"

### Offer Types Shown:
- **China Restaurant Shanghai:** "Abendbuffet" (Evening Buffet), "Mittagsbuffet" (Lunch Buffet)
- **Cetrez Feinkost:** "Lebensmittel" (Groceries)
- **Denns Bio:** "Backwaren" (Baked Goods)

### Current Codebase:
- **Vendor** has `businessType` (e.g., "restaurant", "bakery", "supermarket")
- **Offer** has `businessType` (inherited from vendor when created)
- **No separate category field** for offers

---

## Recommendation: **Categories at OFFER Level** ⭐

### Why Offer-Level Categories?

1. **One Vendor, Multiple Categories**
   - A restaurant can have offers in "Meals" AND "Bread & pastries"
   - Example: China Restaurant has both "Abendbuffet" and "Mittagsbuffet" (both are "Meals" category)
   - A bakery can have "Bread & pastries" AND "Groceries"

2. **More Granular Than Business Type**
   - `businessType` = "restaurant" (vendor-level, broad)
   - `category` = "Meals", "Bread & pastries" (offer-level, specific)
   - Categories are what users filter by, not business types

3. **Flexibility**
   - Vendors can create offers in different categories
   - Same vendor can have different offer types (Lunch vs Evening)
   - Better for user experience (filtering by what they want to buy)

4. **Matches Your UI**
   - Users filter by offer categories (Meals, Groceries, etc.)
   - Not by vendor business type
   - Each offer card shows its category/type

---

## Implementation Approach

### Option 1: Add `category` Field to Offer (Recommended) ⭐

**Structure:**
```
Vendor:
  - businessType: "restaurant" (vendor's main business)

Offer:
  - businessType: "restaurant" (inherited from vendor)
  - category: "Meals" (specific offer category)
```

**Benefits:**
- ✅ Clear separation: business type vs category
- ✅ Vendor can have offers in multiple categories
- ✅ Easy filtering by category
- ✅ Backward compatible (businessType still exists)

**Database:**
```sql
ALTER TABLE offers ADD COLUMN category VARCHAR(50);
```

**Categories Enum/List:**
```java
public enum OfferCategory {
    MEALS("Meals"),
    BREAD_PASTRIES("Bread & pastries"),
    GROCERIES("Groceries"),
    FLOWERS_PLANTS("Flowers & plants"),
    PET_FOOD("Pet food"),
    // Add more as needed
}
```

---

### Option 2: Use `businessType` for Both (Not Recommended)

**Problems:**
- ❌ Vendor can only have one business type
- ❌ Can't have offers in multiple categories
- ❌ Less flexible
- ❌ Doesn't match your UI needs

---

## Recommended Implementation

### 1. Add `category` Field to Offer Entity

```java
@Entity
@Table(name = "offers")
public class Offer {
    // ... existing fields ...
    
    @Column(nullable = false)
    private String businessType; // Keep for backward compatibility
    
    @Column(nullable = true) // Allow null initially for migration
    private String category; // NEW: "Meals", "Groceries", etc.
    
    // ... rest of fields ...
}
```

### 2. Update OfferDto

```java
public class OfferDto {
    // ... existing fields ...
    
    private String businessType;
    private String category; // NEW
    
    // ... rest of fields ...
}
```

### 3. Update Offer Creation

```java
// In VendorService.createOffer()
OfferCreationEvent event = new OfferCreationEvent(
    // ... existing params ...
    request.getCategory() != null ? request.getCategory() : 
        mapBusinessTypeToCategory(vendor.getBusinessType()) // Fallback
);
```

### 4. Add Category Filtering Endpoint

```java
@GetMapping("/offers")
public List<OfferDto> getOffers(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String businessType
) {
    return offerService.getOffers(category, businessType);
}
```

### 5. Category Mapping (Fallback Logic)

```java
private String mapBusinessTypeToCategory(String businessType) {
    // Default category mapping if not provided
    switch (businessType.toLowerCase()) {
        case "restaurant": return "Meals";
        case "bakery": return "Bread & pastries";
        case "supermarket": return "Groceries";
        case "florist": return "Flowers & plants";
        case "pet_shop": return "Pet food";
        default: return "Groceries"; // Default fallback
    }
}
```

---

## Migration Strategy

### Phase 1: Add Category Field (Nullable)
1. Add `category` column to `offers` table (nullable)
2. Update Offer entity and DTO
3. Update creation/update logic

### Phase 2: Populate Existing Data
```sql
-- Set category based on businessType for existing offers
UPDATE offers 
SET category = CASE 
    WHEN business_type = 'restaurant' THEN 'Meals'
    WHEN business_type = 'bakery' THEN 'Bread & pastries'
    WHEN business_type = 'supermarket' THEN 'Groceries'
    ELSE 'Groceries'
END
WHERE category IS NULL;
```

### Phase 3: Make Category Required
1. After migration, make category NOT NULL
2. Ensure all new offers require category

---

## Category List (Based on Your UI)

```java
public enum OfferCategory {
    ALL("All"),                    // Special: shows all
    MEALS("Meals"),
    BREAD_PASTRIES("Bread & pastries"),
    GROCERIES("Groceries"),
    FLOWERS_PLANTS("Flowers & plants"),
    PET_FOOD("Pet food");
    
    private final String displayName;
    
    OfferCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

---

## API Changes Needed

### 1. Offer Creation Request
```json
{
  "name": "Fresh Salad Box",
  "price": 5.99,
  "category": "Meals",  // NEW field
  "businessType": "restaurant",  // Still required (from vendor)
  // ... other fields
}
```

### 2. Filter Endpoints
```http
GET /offers?category=Meals
GET /offers?category=Groceries
GET /offers?category=Meals&businessType=restaurant
```

### 3. Response
```json
{
  "id": 123,
  "name": "Fresh Salad Box",
  "category": "Meals",  // NEW in response
  "businessType": "restaurant",
  // ... other fields
}
```

---

## Summary

**✅ RECOMMENDED: Add `category` field to Offer**

**Reasons:**
1. One vendor can have multiple offer categories
2. More flexible and matches your UI
3. Better user experience (filter by what they want)
4. Categories are offer-specific, not vendor-specific

**Implementation:**
- Add `category` column to offers table
- Update Offer entity and DTO
- Add category to offer creation
- Add category filtering endpoints
- Migrate existing data

**Fallback Strategy:**
- If category not provided, map from `businessType`
- Eventually make category required

---

## Next Steps

1. ✅ Add `category` field to Offer entity
2. ✅ Update OfferDto
3. ✅ Add category to offer creation endpoint
4. ✅ Add category filtering to get offers endpoints
5. ✅ Create migration script for existing data
6. ✅ Update mobile app to send/use category

