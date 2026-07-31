# Currency Localization Implementation

This document describes the implementation of location-based currency detection for offers in the StillFresh system.

## Overview

The system now automatically determines the currency for offers based on the vendor's geographic location (coordinates). This ensures that:
- Vendors see prices in their local currency
- Customers see prices in the vendor's currency
- Payments are processed in the correct currency via Stripe

## Architecture

### Backend Implementation (Recommended Approach)

The currency detection logic is implemented entirely in the backend for:
- **Security**: Prevents currency manipulation
- **Consistency**: Single source of truth for currency rules
- **Maintainability**: Centralized business logic

### Flow

```
1. Vendor creates offer
   ↓
2. Backend receives offer with coordinates (vendor location)
   ↓
3. CurrencyDetectionService reverse geocodes coordinates → country
   ↓
4. Country mapped to Currency enum (e.g., Serbia → RSD, Germany → EUR)
   ↓
5. Currency stored with offer
   ↓
6. Offer returned to frontend with currency
   ↓
7. Order uses offer's currency for payment
```

## Implementation Details

### 1. CurrencyDetectionService

**Location**: `offer-service/src/main/java/com/stillfresh/app/offerservice/service/CurrencyDetectionService.java`

**Features**:
- Reverse geocoding using Google Maps API
- Country-to-currency mapping for European countries
- Fallback to EUR if country cannot be determined
- Supports direct country code to currency mapping

**Supported Countries/Currencies**:
- **Eurozone**: EUR (Austria, Belgium, France, Germany, Italy, Spain, etc.)
- **Serbia**: RSD
- **United Kingdom**: GBP
- **Switzerland**: CHF
- **Sweden**: SEK
- **Norway**: NOK
- **Denmark**: DKK
- **Poland**: PLN
- **Hungary**: HUF
- **Czech Republic**: CZK
- **Romania**: RON
- **Bulgaria**: BGN
- **Iceland**: ISK
- **Albania**: ALL
- **North Macedonia**: MKD
- And more...

### 2. Offer Entity Updates

**Added Field**:
```java
@Column(nullable = false)
private String currency; // ISO currency code (e.g., "EUR", "RSD", "USD")
```

### 3. OfferDto Updates

**Added Field**:
```java
private String currency;  // ISO currency code (e.g., "EUR", "RSD", "USD")
```

### 4. Offer Creation Flow

When an offer is created:
1. `OfferService.createOffer()` is called
2. Currency is determined from offer coordinates using `CurrencyDetectionService`
3. Currency is stored with the offer
4. Offer is saved to database

### 5. Order Processing Flow

When an order is placed:
1. Order service retrieves offer details
2. Currency is extracted from offer (not hardcoded)
3. Payment request uses offer's currency
4. Stripe PaymentIntent is created with correct currency

### 6. Vendor Details Update

When vendor details (including location) are updated:
1. All offers for that vendor are updated
2. Currency is re-determined based on new coordinates
3. Offers are updated with new currency

## Database Migration

**File**: `offer-service/add_currency_column.sql`

```sql
ALTER TABLE offers 
ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'EUR';
```

**Note**: Existing offers will default to EUR. You may want to run currency detection for existing offers.

## Configuration

### Google Maps API Key

Add to `offer-service/src/main/resources/application.yml`:

```yaml
google:
  maps:
    api-key: ${GOOGLE_MAPS_API_KEY:}  # Set via environment variable
```

Or use the same key as vendor-service if shared.

## API Changes

### Offer Response

Offers now include currency in the response:

```json
{
  "id": 123,
  "name": "Fresh Bread",
  "price": 500.00,
  "currency": "RSD",  // ← New field
  "latitude": 44.7866,
  "longitude": 20.4489,
  ...
}
```

### Order Response

Orders include currency from the offer:

```json
{
  "id": 456,
  "offerId": 123,
  "totalPrice": 500.00,
  "currency": "RSD",  // ← From offer
  ...
}
```

## Stripe Integration

Stripe supports multiple currencies natively:
- PaymentIntents are created with the currency from the offer
- Stripe handles the payment in that currency
- No automatic conversion to EUR (each payment uses its specified currency)

**Important**: Ensure Stripe account supports the currencies you're using. Most European currencies are supported.

## Backward Compatibility

- Existing offers without currency default to EUR
- Orders without currency default to RSD (for backward compatibility)
- System gracefully handles missing currency fields

## Testing

### Test Currency Detection

1. Create an offer with coordinates in Serbia → Should detect RSD
2. Create an offer with coordinates in Germany → Should detect EUR
3. Create an offer with coordinates in UK → Should detect GBP

### Test Order Processing

1. Place order for offer with RSD currency → Payment should use RSD
2. Place order for offer with EUR currency → Payment should use EUR

## Future Enhancements

1. **Currency Conversion**: Add exchange rate API for displaying prices in user's preferred currency
2. **User Currency Preference**: Allow users to see prices in their preferred currency (display only)
3. **Multi-Currency Support**: Support more currencies as business expands
4. **Caching**: Cache reverse geocoding results to reduce API calls
5. **Batch Updates**: Update currency for existing offers in bulk

## Notes

- Currency is determined from **vendor's location** (offer coordinates), not user's location
- This ensures payment uses vendor's currency, which is correct for business logic
- Frontend can optionally display prices in user's preferred currency (display only)
- Actual payment always uses offer's currency

