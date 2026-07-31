# Mobile App Favorites Integration Guide

This guide provides comprehensive instructions for integrating the favorites functionality into the mobile app. The favorites feature allows users to save offers they're interested in for later viewing.

## Table of Contents
1. [Overview](#overview)
2. [Authentication](#authentication)
3. [API Endpoints](#api-endpoints)
4. [Request/Response Formats](#requestresponse-formats)
5. [Error Handling](#error-handling)
6. [Best Practices](#best-practices)
7. [Example Implementation](#example-implementation)

---

## Overview

The favorites feature allows authenticated users (customers) to:
- **Add** offers to their favorites list (heart icon)
- **Remove** offers from their favorites list (including expired or sold-out offers)
- **View** all their favorited offers
- **Check** if a specific offer is favorited
- **Get** the total count of favorites
- **Get a summary** of how many favorites are expired or sold out (for in-app notification)

**Expired and sold-out offers in favorites:** Offers that have expired or sold out remain in the list. Each item includes `offer.isExpired`, `offer.isSoldOut`, and `offer.isGreyedOut` so the app can mark them (e.g. greyed out, "Expired" badge) and still allow the user to remove them. Use the **favorites summary** endpoint to show a notification like "You have N expired offers in your favorites."

---

## Authentication

All favorites endpoints require authentication via JWT token.

### How to Authenticate

1. **Login first** to get a JWT token:
   ```
   POST /auth/login
   Body: { "identifier": "user@example.com", "password": "password123" }
   Response: { "token": "eyJhbGciOiJIUzI1NiJ9..." }
   ```

2. **Include token** in all favorites requests:
   ```
   Authorization: Bearer <jwt-token>
   ```

3. **Token expiration**: Tokens expire after 15 minutes. Use the refresh token endpoint if needed.

---

## API Endpoints

All endpoints are accessible through the API Gateway at: `http://api-gateway:8080/users/favorites/*`

### Base URL
```
Production: https://api.stillfresh.app/users/favorites
Development: http://localhost:8080/users/favorites
```

### 1. Add Offer to Favorites

**Endpoint:** `POST /users/favorites/{offerId}`

**Description:** Adds an offer to the user's favorites list. This operation is **idempotent** - if the offer is already favorited, it returns the existing favorite without error.

**Request:**
```http
POST /users/favorites/123
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

**Path Parameters:**
- `offerId` (Long, required) - The ID of the offer to favorite

**Success Response (200 OK):**
```json
{
  "favoriteId": 456,
  "offerId": 123,
  "favoritedAt": "2025-11-26T12:00:00Z",
  "offer": {
    "id": 123,
    "name": "Fresh Organic Salad",
    "description": "Delicious organic salad mix",
    "price": 5.99,
    "originalPrice": 9.99,
    "vendorName": "Green Market",
    "imageUrl": "https://example.com/salad.jpg",
    "rating": 4.5,
    "reviewsCount": 120,
    "active": true,
    "expirationDate": "2025-11-27T18:00:00Z",
    "address": "123 Main St",
    "latitude": 49.3723853,
    "longitude": 8.6776575,
    ...
  }
}
```

**Error Responses:**
- `401 UNAUTHORIZED` - Missing or invalid JWT token
- `403 FORBIDDEN` - User doesn't have USER role
- `404 NOT_FOUND` - Offer doesn't exist
- `500 INTERNAL_SERVER_ERROR` - Server error

**Mobile App Implementation:**
```dart
// Flutter/Dart example
Future<FavoriteResponse> addFavorite(int offerId) async {
  final response = await http.post(
    Uri.parse('$baseUrl/users/favorites/$offerId'),
    headers: {
      'Authorization': 'Bearer $jwtToken',
      'Content-Type': 'application/json',
    },
  );
  
  if (response.statusCode == 200) {
    return FavoriteResponse.fromJson(json.decode(response.body));
  } else {
    throw Exception('Failed to add favorite: ${response.statusCode}');
  }
}
```

---

### 2. Remove Offer from Favorites

**Endpoint:** `DELETE /users/favorites/{offerId}`

**Description:** Removes an offer from the user's favorites list.

**Request:**
```http
DELETE /users/favorites/123
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Path Parameters:**
- `offerId` (Long, required) - The ID of the offer to remove from favorites

**Note:** Removal works for any favorited offer, including expired or sold-out offers. The operation is **idempotent**: if the offer is not in favorites, the API still returns 200 (desired state is already achieved).

**Success Response (200 OK):** Body is a plain string: `"Offer removed from favorites successfully"`

**Error Responses:**
- `401 UNAUTHORIZED` - Missing or invalid JWT token
- `403 FORBIDDEN` - User doesn't have USER role
- `500 INTERNAL_SERVER_ERROR` - Server error

**Mobile App Implementation:**
```dart
Future<void> removeFavorite(int offerId) async {
  final response = await http.delete(
    Uri.parse('$baseUrl/users/favorites/$offerId'),
    headers: {
      'Authorization': 'Bearer $jwtToken',
    },
  );
  
  if (response.statusCode != 200) {
    throw Exception('Failed to remove favorite: ${response.statusCode}');
  }
}
```

---

### 3. Get User's Favorites

**Endpoint:** `GET /users/favorites`

**Description:** Retrieves offers that the user has favorited, with full offer details. Supports pagination. Response includes summary counts so the app can notify the user about expired or sold-out favorites. Each offer in the list includes `isExpired`, `isSoldOut`, and `isGreyedOut` so expired/sold-out items can be shown as such and removed.

**Request:**
```http
GET /users/favorites?page=0&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Query Parameters:**
- `page` (int, optional, default: 0) - Page number (0-indexed)
- `size` (int, optional, default: 20) - Number of items per page

**Note:** If you want all favorites without pagination, use `page=0` and `size=2147483647` (Integer.MAX_VALUE).

**Success Response (200 OK):** The response is an object (not a bare array) with the following fields:

```json
{
  "favorites": [
    {
      "favoriteId": 456,
      "offerId": 123,
      "favoritedAt": "2025-11-26T12:00:00Z",
      "offer": {
        "id": 123,
        "name": "Fresh Organic Salad",
        "description": "Delicious organic salad mix",
        "price": 5.99,
        "originalPrice": 9.99,
        "vendorName": "Green Market",
        "imageUrl": "https://example.com/salad.jpg",
        "rating": 4.5,
        "reviewsCount": 120,
        "active": true,
        "expirationDate": "2025-11-27T18:00:00Z",
        "isExpired": false,
        "isSoldOut": false,
        "isGreyedOut": false,
        ...
      }
    },
    {
      "favoriteId": 457,
      "offerId": 124,
      "favoritedAt": "2025-11-26T11:30:00Z",
      "offer": {
        "id": 124,
        "name": "Fresh Bread",
        "isExpired": true,
        "isSoldOut": false,
        "isGreyedOut": true,
        ...
      }
    }
  ],
  "totalCount": 2,
  "expiredCount": 1,
  "soldOutCount": 0
}
```

**Response fields:**
- `favorites` (array) - List of favorite items (each with `favoriteId`, `offerId`, `favoritedAt`, `offer`). When paginated, this is the current page.
- `totalCount` (number) - Total number of favorites (full total when paginated).
- `expiredCount` (number) - Count of expired offers in the returned set (full list if unpaginated, current page if paginated).
- `soldOutCount` (number) - Count of sold-out offers in the returned set.

**Offer status flags (inside each `offer`):**
- `isExpired` (boolean) - Offer has expired; show as "Expired" and allow removal.
- `isSoldOut` (boolean) - Offer is sold out; show as "Sold out" and allow removal.
- `isGreyedOut` (boolean) - `true` if `isExpired` or `isSoldOut`; use for greying out the row/card.

**Response is ordered by:** Most recently favorited first (descending by `favoritedAt`)

**Error Responses:**
- `401 UNAUTHORIZED` - Missing or invalid JWT token
- `403 FORBIDDEN` - User doesn't have USER role
- `500 INTERNAL_SERVER_ERROR` - Server error

**Mobile App Implementation:**
```dart
Future<FavoritesListResponse> getFavorites({int page = 0, int size = 20}) async {
  final response = await http.get(
    Uri.parse('$baseUrl/users/favorites?page=$page&size=$size'),
    headers: {
      'Authorization': 'Bearer $jwtToken',
    },
  );

  if (response.statusCode == 200) {
    final json = json.decode(response.body);
    return FavoritesListResponse.fromJson(json);
  } else {
    throw Exception('Failed to get favorites: ${response.statusCode}');
  }
}
// Show notification when expiredCount > 0: "You have N expired offer(s) in your favorites."
```

---

### 3b. Get Favorites Summary (for notification)

**Endpoint:** `GET /users/favorites/summary`

**Description:** Returns total count and counts of expired and sold-out offers in the user's favorites. Use this to show a banner or notification such as "You have N expired offers in your favorites" without loading the full list.

**Request:**
```http
GET /users/favorites/summary
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Success Response (200 OK):**
```json
{
  "favorites": [],
  "totalCount": 10,
  "expiredCount": 2,
  "soldOutCount": 1
}
```

**Response fields:** `totalCount`, `expiredCount`, `soldOutCount`. The `favorites` array is empty in this response.

---

### 4. Check if Offer is Favorited

**Endpoint:** `GET /users/favorites/{offerId}`

**Description:** Checks if a specific offer is in the user's favorites list. Useful for displaying the heart icon state.

**Request:**
```http
GET /users/favorites/123
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Path Parameters:**
- `offerId` (Long, required) - The ID of the offer to check

**Success Response (200 OK):**
```json
{
  "isFavorited": true,
  "offerId": 123
}
```

**Response Fields:**
- `isFavorited` (boolean) - `true` if the offer is favorited, `false` otherwise
- `offerId` (Long) - The offer ID that was checked

**Error Responses:**
- `401 UNAUTHORIZED` - Missing or invalid JWT token
- `403 FORBIDDEN` - User doesn't have USER role
- `500 INTERNAL_SERVER_ERROR` - Server error

**Mobile App Implementation:**
```dart
Future<bool> isFavorited(int offerId) async {
  final response = await http.get(
    Uri.parse('$baseUrl/users/favorites/$offerId'),
    headers: {
      'Authorization': 'Bearer $jwtToken',
    },
  );
  
  if (response.statusCode == 200) {
    final json = json.decode(response.body);
    return json['isFavorited'] as bool;
  } else {
    throw Exception('Failed to check favorite status: ${response.statusCode}');
  }
}
```

---

### 5. Get Favorites Count

**Endpoint:** `GET /users/favorites/count`

**Description:** Returns the total number of offers the user has favorited. Useful for displaying a badge or count indicator.

**Request:**
```http
GET /users/favorites/count
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Success Response (200 OK):**
```json
{
  "count": 15
}
```

**Response Fields:**
- `count` (Long) - Total number of favorites

**Error Responses:**
- `401 UNAUTHORIZED` - Missing or invalid JWT token
- `403 FORBIDDEN` - User doesn't have USER role
- `500 INTERNAL_SERVER_ERROR` - Server error

**Mobile App Implementation:**
```dart
Future<int> getFavoriteCount() async {
  final response = await http.get(
    Uri.parse('$baseUrl/users/favorites/count'),
    headers: {
      'Authorization': 'Bearer $jwtToken',
    },
  );
  
  if (response.statusCode == 200) {
    final json = json.decode(response.body);
    return json['count'] as int;
  } else {
    throw Exception('Failed to get favorite count: ${response.statusCode}');
  }
}
```

---

## Request/Response Formats

### Common Headers

All requests must include:
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

### Date/Time Format

All timestamps are in ISO 8601 format with timezone:
```
2025-11-26T12:00:00Z
```

### Offer Object Structure

The `offer` field in `FavoriteResponse` contains a full `OfferDto` with the following key fields:

```json
{
  "id": 123,
  "name": "Offer Name",
  "description": "Offer description",
  "price": 5.99,
  "originalPrice": 9.99,
  "quantityAvailable": 10,
  "vendorId": 456,
  "vendorName": "Vendor Name",
  "imageUrl": "https://example.com/image.jpg",
  "rating": 4.5,
  "reviewsCount": 120,
  "active": true,
  "expirationDate": "2025-11-27T18:00:00Z",
  "createdAt": "2025-11-25T10:00:00Z",
  "address": "123 Main St",
  "zipCode": "12345",
  "latitude": 49.3723853,
  "longitude": 8.6776575,
  "businessType": "RESTAURANT",
  "dietaryInfo": "Vegetarian, 250 calories",
  "allergenInfo": "Contains gluten",
  "pickupStartTime": "10:00:00",
  "pickupEndTime": "18:00:00"
}
```

---

## Error Handling

### Common HTTP Status Codes

| Status Code | Meaning | Action |
|------------|---------|--------|
| `200 OK` | Request successful | Process response data |
| `401 UNAUTHORIZED` | Invalid or missing JWT token | Redirect to login, refresh token |
| `403 FORBIDDEN` | User doesn't have required role | Show error message, check user role |
| `404 NOT_FOUND` | Resource not found (offer/favorite) | Show "Not found" message |
| `500 INTERNAL_SERVER_ERROR` | Server error | Show generic error, retry later |

### Error Response Format

```json
{
  "error": "Error message description",
  "timestamp": "2025-11-26T12:00:00Z",
  "status": 404,
  "path": "/users/favorites/999"
}
```

### Mobile App Error Handling

```dart
try {
  await addFavorite(offerId);
  // Show success message
  showSnackBar('Added to favorites');
} on UnauthorizedException {
  // Token expired or invalid
  await refreshToken();
  // Retry the operation
  await addFavorite(offerId);
} on NotFoundException {
  // Offer doesn't exist
  showSnackBar('Offer not found');
} on Exception catch (e) {
  // Generic error
  showSnackBar('Failed to add favorite: ${e.message}');
}
```

---

## Best Practices

### 1. **Optimistic UI Updates**

Update the UI immediately when user taps the heart icon, then sync with server:

```dart
void toggleFavorite(Offer offer) {
  // Optimistic update
  setState(() {
    offer.isFavorited = !offer.isFavorited;
  });
  
  // Sync with server
  if (offer.isFavorited) {
    addFavorite(offer.id).catchError((error) {
      // Revert on error
      setState(() {
        offer.isFavorited = false;
      });
      showError('Failed to add favorite');
    });
  } else {
    removeFavorite(offer.id).catchError((error) {
      // Revert on error
      setState(() {
        offer.isFavorited = true;
      });
      showError('Failed to remove favorite');
    });
  }
}
```

### 2. **Batch Check Favorites Status**

When displaying a list of offers, check favorites status in bulk:

```dart
// Get all favorite offer IDs once
final favoriteOfferIds = await getFavoriteOfferIds();

// Use for quick lookup
bool isFavorited = favoriteOfferIds.contains(offer.id);
```

**Note:** This endpoint doesn't exist yet, but you can use `GET /users/favorites` and extract offer IDs, or implement a bulk check endpoint if needed.

### 3. **Cache Favorites**

Cache the favorites list locally to:
- Show favorites immediately (offline support)
- Reduce API calls
- Improve performance

```dart
// Store favorites in local database/cache
await cacheFavorites(favorites);

// Load from cache first, then refresh from server
final cachedFavorites = await getCachedFavorites();
if (cachedFavorites.isNotEmpty) {
  displayFavorites(cachedFavorites);
}
refreshFavoritesFromServer();
```

### 4. **Handle Network Errors Gracefully**

```dart
Future<void> addFavoriteWithRetry(int offerId, {int maxRetries = 3}) async {
  for (int i = 0; i < maxRetries; i++) {
    try {
      await addFavorite(offerId);
      return; // Success
    } catch (e) {
      if (i == maxRetries - 1) {
        throw e; // Last retry failed
      }
      await Future.delayed(Duration(seconds: 2 * (i + 1))); // Exponential backoff
    }
  }
}
```

### 5. **Pagination for Large Lists**

Always use pagination when fetching favorites:

```dart
int currentPage = 0;
int pageSize = 20;
bool hasMore = true;

Future<void> loadMoreFavorites() async {
  if (!hasMore) return;
  
  final favorites = await getFavorites(page: currentPage, size: pageSize);
  
  if (favorites.length < pageSize) {
    hasMore = false;
  }
  
  setState(() {
    allFavorites.addAll(favorites);
    currentPage++;
  });
}
```

### 6. **Heart Icon States**

Display appropriate heart icon based on favorite status:

```dart
IconButton(
  icon: Icon(
    offer.isFavorited ? Icons.favorite : Icons.favorite_border,
    color: offer.isFavorited ? Colors.red : Colors.grey,
  ),
  onPressed: () => toggleFavorite(offer),
)
```

### 7. **Loading States**

Show loading indicators during API calls:

```dart
bool isLoading = false;

Future<void> addFavorite(int offerId) async {
  setState(() => isLoading = true);
  try {
    await favoriteService.addFavorite(offerId);
  } finally {
    setState(() => isLoading = false);
  }
}
```

---

## Example Implementation

### Complete Flutter/Dart Example

```dart
class FavoritesService {
  final String baseUrl = 'http://localhost:8080';
  final String jwtToken;
  
  FavoritesService(this.jwtToken);
  
  // Add to favorites
  Future<FavoriteResponse> addFavorite(int offerId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/users/favorites/$offerId'),
      headers: {
        'Authorization': 'Bearer $jwtToken',
        'Content-Type': 'application/json',
      },
    );
    
    if (response.statusCode == 200) {
      return FavoriteResponse.fromJson(json.decode(response.body));
    } else if (response.statusCode == 401) {
      throw UnauthorizedException('Token expired');
    } else if (response.statusCode == 404) {
      throw NotFoundException('Offer not found');
    } else {
      throw Exception('Failed to add favorite: ${response.statusCode}');
    }
  }
  
  // Remove from favorites
  Future<void> removeFavorite(int offerId) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/users/favorites/$offerId'),
      headers: {
        'Authorization': 'Bearer $jwtToken',
      },
    );
    
    if (response.statusCode != 200) {
      if (response.statusCode == 401) {
        throw UnauthorizedException('Token expired');
      } else if (response.statusCode == 404) {
        throw NotFoundException('Favorite not found');
      } else {
        throw Exception('Failed to remove favorite: ${response.statusCode}');
      }
    }
  }
  
  // Get all favorites
  Future<List<FavoriteResponse>> getFavorites({int page = 0, int size = 20}) async {
    final response = await http.get(
      Uri.parse('$baseUrl/users/favorites?page=$page&size=$size'),
      headers: {
        'Authorization': 'Bearer $jwtToken',
      },
    );
    
    if (response.statusCode == 200) {
      final List<dynamic> jsonList = json.decode(response.body);
      return jsonList.map((json) => FavoriteResponse.fromJson(json)).toList();
    } else if (response.statusCode == 401) {
      throw UnauthorizedException('Token expired');
    } else {
      throw Exception('Failed to get favorites: ${response.statusCode}');
    }
  }
  
  // Check if favorited
  Future<bool> isFavorited(int offerId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/users/favorites/$offerId'),
      headers: {
        'Authorization': 'Bearer $jwtToken',
      },
    );
    
    if (response.statusCode == 200) {
      final json = json.decode(response.body);
      return json['isFavorited'] as bool;
    } else if (response.statusCode == 401) {
      throw UnauthorizedException('Token expired');
    } else {
      throw Exception('Failed to check favorite status: ${response.statusCode}');
    }
  }
  
  // Get count
  Future<int> getFavoriteCount() async {
    final response = await http.get(
      Uri.parse('$baseUrl/users/favorites/count'),
      headers: {
        'Authorization': 'Bearer $jwtToken',
      },
    );
    
    if (response.statusCode == 200) {
      final json = json.decode(response.body);
      return json['count'] as int;
    } else if (response.statusCode == 401) {
      throw UnauthorizedException('Token expired');
    } else {
      throw Exception('Failed to get favorite count: ${response.statusCode}');
    }
  }
}

// Model classes
class FavoriteResponse {
  final int favoriteId;
  final int offerId;
  final DateTime favoritedAt;
  final OfferDto? offer;
  
  FavoriteResponse({
    required this.favoriteId,
    required this.offerId,
    required this.favoritedAt,
    this.offer,
  });
  
  factory FavoriteResponse.fromJson(Map<String, dynamic> json) {
    return FavoriteResponse(
      favoriteId: json['favoriteId'],
      offerId: json['offerId'],
      favoritedAt: DateTime.parse(json['favoritedAt']),
      offer: json['offer'] != null ? OfferDto.fromJson(json['offer']) : null,
    );
  }
}

class OfferDto {
  final int id;
  final String name;
  final String description;
  final double price;
  final double originalPrice;
  final String vendorName;
  final String? imageUrl;
  final double rating;
  final int reviewsCount;
  final bool active;
  final DateTime? expirationDate;
  // ... other fields
  
  OfferDto.fromJson(Map<String, dynamic> json)
      : id = json['id'],
        name = json['name'],
        description = json['description'],
        price = json['price'].toDouble(),
        originalPrice = json['originalPrice'].toDouble(),
        vendorName = json['vendorName'],
        imageUrl = json['imageUrl'],
        rating = json['rating'].toDouble(),
        reviewsCount = json['reviewsCount'],
        active = json['active'],
        expirationDate = json['expirationDate'] != null 
            ? DateTime.parse(json['expirationDate']) 
            : null;
}
```

### UI Component Example

```dart
class FavoriteButton extends StatefulWidget {
  final int offerId;
  final bool initialFavoriteState;
  
  const FavoriteButton({
    required this.offerId,
    this.initialFavoriteState = false,
  });
  
  @override
  _FavoriteButtonState createState() => _FavoriteButtonState();
}

class _FavoriteButtonState extends State<FavoriteButton> {
  late bool isFavorited;
  bool isLoading = false;
  final favoritesService = FavoritesService(getJwtToken());
  
  @override
  void initState() {
    super.initState();
    isFavorited = widget.initialFavoriteState;
  }
  
  Future<void> toggleFavorite() async {
    if (isLoading) return;
    
    setState(() {
      isLoading = true;
      isFavorited = !isFavorited; // Optimistic update
    });
    
    try {
      if (isFavorited) {
        await favoritesService.addFavorite(widget.offerId);
      } else {
        await favoritesService.removeFavorite(widget.offerId);
      }
    } catch (e) {
      // Revert on error
      setState(() {
        isFavorited = !isFavorited;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to update favorite: $e')),
      );
    } finally {
      setState(() {
        isLoading = false;
      });
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: isLoading
          ? CircularProgressIndicator()
          : Icon(
              isFavorited ? Icons.favorite : Icons.favorite_border,
              color: isFavorited ? Colors.red : Colors.grey,
            ),
      onPressed: toggleFavorite,
    );
  }
}
```

---

## Testing Checklist

Before releasing to production, ensure:

- [ ] Add favorite works correctly
- [ ] Remove favorite works correctly
- [ ] Get favorites list displays correctly
- [ ] Check favorite status works correctly
- [ ] Favorite count is accurate
- [ ] Heart icon updates immediately (optimistic UI)
- [ ] Error handling works (network errors, 401, 404, etc.)
- [ ] Token expiration is handled gracefully
- [ ] Pagination works for large favorite lists
- [ ] Offline behavior is handled (if caching is implemented)
- [ ] Duplicate favorites are prevented (idempotent add)
- [ ] Favorites are user-specific (user A can't see user B's favorites)

---

## Support

For issues or questions:
- Check API Gateway logs for request/response details
- Verify JWT token is valid and not expired
- Ensure user has `USER` role (not `VENDOR` or `ADMIN`)
- Check network connectivity
- Verify offer IDs are valid

---

## Version History

- **v1.0** (2025-11-26) - Initial implementation
  - Add/remove favorites
  - Get favorites list with pagination
  - Check favorite status
  - Get favorites count

