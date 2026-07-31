# Category Implementation Summary

## Overview
Category functionality has been successfully implemented with multi-language support for Serbia, Croatia, Montenegro, Bosnia, Slovenia, Bulgaria, Romania, and Macedonia.

## What Was Implemented

### 1. **OfferCategory Enum** (`shared-entities`)
- Created enum with 6 categories: ALL, MEALS, BREAD_PASTRIES, GROCERIES, FLOWERS_PLANTS, PET_FOOD
- Includes translations for 8 languages:
  - English (en) - default
  - Serbian (sr)
  - Croatian (hr)
  - Montenegrin (me)
  - Bosnian (bs)
  - Slovenian (sl)
  - Bulgarian (bg)
  - Romanian (ro)
  - Macedonian (mk)
- Automatic fallback to English if locale not supported

### 2. **Database Changes**
- Added `category` column to `offers` table (nullable for migration)
- Migration script: `offer-service/add_category_column.sql`
- Existing offers get category mapped from `businessType`

### 3. **Entity Updates**
- ✅ `Offer` entity - added `category` field (OfferCategory enum)
- ✅ `OfferDto` - added `category` field
- ✅ `OfferCreationEvent` - added `category` field
- ✅ `OfferUpdateEvent` - category included via OfferDto

### 4. **Service Layer**
- ✅ `OfferService.createOffer()` - handles category from event, with fallback mapping
- ✅ `OfferService.updateOffer()` - updates category from DTO
- ✅ `OfferService.toOfferDto()` - includes category in DTO conversion
- ✅ `OfferService.mapBusinessTypeToCategory()` - fallback mapping helper
- ✅ `OfferService.getOffersByCategory()` - new method for category filtering
- ✅ `OfferService.getNearbyOffers()` - updated to support category filtering

### 5. **Repository Layer**
- ✅ Added `findByCategory(OfferCategory category)`
- ✅ Added `findByCategoryAndActive(OfferCategory category, boolean active)`
- ✅ Added `findByVendorIdAndCategory(Long vendorId, OfferCategory category)`

### 6. **API Endpoints**

#### Get Categories (with localization)
```
GET /offers/categories?locale=sr
```
**Response:**
```json
[
  { "value": "MEALS", "displayName": "Obroci" },
  { "value": "GROCERIES", "displayName": "Namirnice" },
  ...
]
```

#### Get All Offers (with category filter)
```
GET /offers?category=MEALS
```

#### Get Nearby Offers (with category filter)
```
GET /offers/nearby?latitude=44.7866&longitude=20.4489&range=10&category=MEALS
```

### 7. **Vendor Service Updates**
- ✅ `VendorService.createOffer()` - passes category from request
- ✅ `VendorService.updateOffer()` - includes category in update

## Language Detection

The system uses locale parameter from the request:
- Frontend sends `locale` parameter (e.g., "sr", "hr", "bg")
- Backend extracts language code (handles both "sr" and "sr-RS" formats)
- Returns translated category names
- Falls back to English if locale not supported

## Category Fallback Logic

1. **If category provided in request:** Use it
2. **If category not provided:** Map from `businessType`:
   - restaurant/cafe/bistro → MEALS
   - bakery/bread/pastry → BREAD_PASTRIES
   - supermarket/grocery/market → GROCERIES
   - florist/flower/plant → FLOWERS_PLANTS
   - pet/animal → PET_FOOD
   - default → GROCERIES

## Database Migration

Run the migration script:
```sql
-- Located at: offer-service/add_category_column.sql
```

The script:
1. Adds `category` column (nullable)
2. Populates existing offers based on `businessType`
3. Leaves column nullable for gradual migration

## Usage Examples

### Creating Offer with Category
```json
POST /vendors/offer-create
{
  "name": "Fresh Salad",
  "price": 5.99,
  "category": "MEALS",  // Optional - will map from businessType if not provided
  ...
}
```

### Getting Categories for UI
```typescript
// Frontend
const locale = getUserLocale(); // "sr", "hr", etc.
const categories = await fetch(`/api/offers/categories?locale=${locale}`);
// Returns: [{ value: "MEALS", displayName: "Obroci" }, ...]
```

### Filtering Offers by Category
```typescript
// Get all meals
const meals = await fetch('/api/offers?category=MEALS');

// Get nearby groceries
const groceries = await fetch('/api/offers/nearby?lat=44.7866&lon=20.4489&range=10&category=GROCERIES');
```

## Supported Languages

| Language | Code | Status |
|----------|------|--------|
| English | en | ✅ Default |
| Serbian | sr | ✅ |
| Croatian | hr | ✅ |
| Montenegrin | me | ✅ |
| Bosnian | bs | ✅ |
| Slovenian | sl | ✅ |
| Bulgarian | bg | ✅ |
| Romanian | ro | ✅ |
| Macedonian | mk | ✅ |

## Next Steps

1. **Run Migration:**
   ```bash
   psql -d stillfresh_offerdb -f offer-service/add_category_column.sql
   ```

2. **Update Frontend:**
   - Fetch categories from `/offers/categories?locale={userLocale}`
   - Include category in offer creation form
   - Use category for filtering offers

3. **Optional: Make Category Required**
   - After all existing offers have categories
   - Update migration to set NOT NULL constraint

## Notes

- Category is stored as enum value (language-agnostic)
- Translations are provided via API endpoint
- Frontend handles locale detection and sends to backend
- Backend validates category enum values
- Fallback to English if locale not supported
- Fallback to businessType mapping if category not provided

