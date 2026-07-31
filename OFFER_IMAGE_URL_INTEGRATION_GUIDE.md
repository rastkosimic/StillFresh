# Offer Image URL Integration Guide

## Overview
When creating an offer, the system supports vendor-provided images. If no image is provided, the system automatically falls back to the vendor's profile image.

## Offer Creation - Image URL Handling

### What to Include in Offer Creation Request

When creating an offer via `POST /vendors/offer-create`, include the `imageUrl` field in the request body:

```json
{
  "name": "Fresh Salad Box",
  "description": "Mixed greens with seasonal vegetables",
  "price": 5.99,
  "originalPrice": 12.99,
  "quantityAvailable": 10,
  "dietaryInfo": "Vegetarian, Gluten-free",
  "allergenInfo": "Contains nuts",
  "pickupStartTime": "14:00:00",
  "pickupEndTime": "18:00:00",
  "imageUrl": "https://example.com/offer-image.jpg",  // OPTIONAL - vendor's offer image
  "rating": 4.5,
  "reviewsCount": 20,
  "expirationDate": "2024-12-31T23:59:59Z"
}
```

### Image URL Field Behavior

1. **If `imageUrl` is provided:**
   - The provided URL is used for the offer
   - Example: `"imageUrl": "https://example.com/salad-box.jpg"`

2. **If `imageUrl` is NOT provided (null, empty, or missing):**
   - The system automatically uses the vendor's profile `imageUrl`
   - The vendor's profile image is copied to the offer
   - No action needed from the client - this happens automatically on the backend

### Implementation Details

**Backend Logic (already implemented):**
```java
// In VendorService.createOffer()
String imageUrl = request.getImageUrl();
if (imageUrl == null || imageUrl.trim().isEmpty()) {
    imageUrl = vendor.getImageUrl();  // Fallback to vendor's profile image
}
```

### Best Practices for AI Agent

1. **Check if vendor has uploaded an offer image:**
   - If the vendor provides an image URL for the offer, include it in the `imageUrl` field
   - This allows vendors to have different images for different offers

2. **If no offer image is available:**
   - Simply omit the `imageUrl` field from the request, OR
   - Send `null` or empty string
   - The backend will automatically use the vendor's profile image

3. **Image URL format:**
   - Should be a valid HTTP/HTTPS URL
   - Example: `"https://storage.example.com/images/offer-123.jpg"`
   - The system does not validate the URL format, but it should be accessible

### Example Request Scenarios

**Scenario 1: Offer with custom image**
```json
POST /vendors/offer-create
{
  "name": "Pizza Box",
  "description": "Leftover pizza slices",
  "price": 8.50,
  "originalPrice": 15.00,
  "quantityAvailable": 5,
  "imageUrl": "https://cdn.example.com/pizza-box.jpg",  // Custom offer image
  "pickupStartTime": "16:00:00",
  "pickupEndTime": "20:00:00"
}
```
**Result:** Offer uses the provided pizza-box.jpg image

**Scenario 2: Offer without image (uses vendor profile image)**
```json
POST /vendors/offer-create
{
  "name": "Bread Basket",
  "description": "Assorted breads",
  "price": 3.99,
  "originalPrice": 7.99,
  "quantityAvailable": 8,
  // imageUrl field omitted
  "pickupStartTime": "10:00:00",
  "pickupEndTime": "14:00:00"
}
```
**Result:** Offer automatically uses vendor's profile imageUrl

### Response

All offer endpoints return `OfferDto` which includes the `imageUrl` field:

```json
{
  "id": 123,
  "vendorId": 45,
  "vendorName": "Bakery Shop",
  "name": "Fresh Salad Box",
  "description": "Mixed greens with seasonal vegetables",
  "price": 5.99,
  "originalPrice": 12.99,
  "quantityAvailable": 10,
  "imageUrl": "https://example.com/offer-image.jpg",  // Always included in response
  "rating": 4.5,
  "reviewsCount": 20,
  "expirationDate": "2024-12-31T23:59:59Z",
  "active": true,
  "address": "123 Main St",
  "zipCode": "12345",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "currency": "EUR",
  "businessType": "Restaurant",
  "pickupStartTime": "14:00:00",
  "pickupEndTime": "18:00:00"
}
```

### Endpoints That Return imageUrl

All these endpoints return offers with `imageUrl` included:
- `GET /offers` - All offers
- `GET /offers/{id}` - Single offer by ID
- `GET /offers/{vendorId}/active` - Active offers for vendor
- `GET /offers/{vendorId}/all-offers` - All offers for vendor
- `GET /offers/nearby` - Nearby offers
- `GET /vendors/active-offers` - Authenticated vendor's active offers
- `GET /vendors/all-offers` - Authenticated vendor's all offers

### Summary for AI Agent

**When creating an offer:**
1. ✅ Include `imageUrl` field if vendor provides a specific image for the offer
2. ✅ Omit `imageUrl` field (or send null/empty) if vendor doesn't provide an offer image
3. ✅ The backend automatically uses vendor's profile image as fallback
4. ✅ All offer responses will include the `imageUrl` field

**Key Point:** The `imageUrl` field is **optional** in the request. If not provided, the system handles the fallback automatically.

