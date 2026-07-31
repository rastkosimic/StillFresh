# Mobile App AI Agent Prompt: Vendor Rating System Integration

## Overview
You are an AI agent responsible for implementing the vendor rating system in the StillFresh mobile application. This guide explains how to integrate the rating functionality that allows users to rate vendors with 4 categories: collection process, quality, quantity, and variety of food. Each category is rated from 1 to 5 stars, and the total rating is the average of these 4 categories.

**Key Concept**: 
- Ratings are **per completed order** (one rating per order pickup)
- The same user can rate the **same vendor multiple times** across different orders; all ratings aggregate into the vendor average
- Resubmitting a rating for the **same order** updates that order's rating (does not create a duplicate)
- Rating includes 4 categories, each rated 1-5 stars:
  - **Collection Process**: How smooth was the pickup process?
  - **Quality**: How was the quality of the food?
  - **Quantity**: Was the quantity satisfactory?
  - **Variety**: Was there good variety in the food?
- Total rating = average of the 4 category ratings (automatically calculated)
- Ratings automatically update the vendor's average rating displayed on their profile

## Base API Configuration
- **Base URL**: `http://localhost:8080` (development) or your production API Gateway URL
- **Authentication**: JWT Bearer token required for all rating endpoints
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

**⚠️ IMPORTANT**: All requests must go through the API Gateway, not directly to individual services.

## Authentication

Before submitting or viewing ratings, the user must be authenticated. Include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

If you receive a `401 Unauthorized` response, the token has expired. Redirect the user to the login screen.

---

## API Endpoints

### 1. Submit a Rating

**Endpoint**: `POST /vendors/ratings/submit`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "vendorId": 123,
  "collectionProcessRating": 5,
  "qualityRating": 4,
  "quantityRating": 4,
  "varietyRating": 3,
  "orderId": 456
}
```

**Request Fields**:
- `vendorId` (number, required): The ID of the vendor being rated
- `collectionProcessRating` (number, required): Rating 1-5 for collection/pickup process
- `qualityRating` (number, required): Rating 1-5 for food quality
- `quantityRating` (number, required): Rating 1-5 for food quantity
- `varietyRating` (number, required): Rating 1-5 for food variety
- `orderId` (number, **required**): The completed order that triggered this rating

**Validation Rules**:
- All rating values must be between 1 and 5 (inclusive)
- All category ratings are required
- `orderId` is required
- Order must be in `COMPLETED` status and belong to the authenticated user
- `vendorId` must match the order's vendor

**Success Response (200 OK)**:
```json
{
  "id": 789,
  "vendorId": 123,
  "userId": 101,
  "orderId": 456,
  "collectionProcessRating": 5,
  "qualityRating": 4,
  "quantityRating": 4,
  "varietyRating": 3,
  "totalRating": 4.0,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**Response Fields**:
- `id`: Rating ID
- `vendorId`: Vendor being rated
- `userId`: User who submitted the rating (current user)
- `orderId`: The completed order this rating is tied to
- `collectionProcessRating`: Collection process rating (1-5)
- `qualityRating`: Quality rating (1-5)
- `quantityRating`: Quantity rating (1-5)
- `varietyRating`: Variety rating (1-5)
- `totalRating`: Average of the 4 ratings (calculated automatically)
- `createdAt`: When the rating was first created (ISO 8601 format)
- `updatedAt`: When the rating was last updated (null if never updated, ISO 8601 format)

**Error Responses**:
- **400 Bad Request**: 
  - Validation errors (e.g., rating out of range, missing required fields)
  - Order not `COMPLETED` or vendor ID mismatch
  - `{"success": false, "message": "Order must be COMPLETED before rating. Current status: CONFIRMED"}`
- **401 Unauthorized**: Token expired or invalid - redirect to login
- **403 Forbidden**: Order does not belong to the authenticated user
- **404 Not Found**: 
  - `{"success": false, "message": "Vendor not found with ID: 123"}`
  - `{"success": false, "message": "Order not found with ID: 456"}`
- **500 Internal Server Error**: Server error - retry or show error message

**Implementation Notes**:
- Prompt the rating UI after an order reaches `COMPLETED` (after `confirm-pickup` and payment capture)
- If the user resubmits for the same `orderId`, that order's rating is **updated** (not duplicated)
- Each new completed order creates a **new** rating row; vendor averages aggregate all order ratings
- The vendor's average rating is automatically recalculated after submission
- All rating values must be integers between 1 and 5

---

### 2. Get All Ratings for a Vendor

**Endpoint**: `GET /vendors/ratings/vendor/{vendorId}`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `vendorId` (number, required): The ID of the vendor

**Success Response (200 OK)**:
```json
[
  {
    "id": 789,
    "vendorId": 123,
    "userId": 101,
    "orderId": 456,
    "collectionProcessRating": 5,
    "qualityRating": 4,
    "quantityRating": 4,
    "varietyRating": 3,
    "totalRating": 4.0,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": null
  },
  {
    "id": 790,
    "vendorId": 123,
    "userId": 102,
    "orderId": 457,
    "collectionProcessRating": 4,
    "qualityRating": 5,
    "quantityRating": 5,
    "varietyRating": 4,
    "totalRating": 4.5,
    "createdAt": "2024-01-16T14:20:00Z",
    "updatedAt": null
  }
]
```

**Error Responses**:
- **401 Unauthorized**: Token expired or invalid
- **500 Internal Server Error**: Server error

**Use Cases**:
- Display all ratings on vendor profile page
- Show rating history/reviews list
- Allow users to browse what others have rated

---

### 3. Get Current User's Ratings

**Endpoint**: `GET /vendors/ratings/my-ratings`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK)**:
```json
[
  {
    "id": 789,
    "vendorId": 123,
    "userId": 101,
    "orderId": 456,
    "collectionProcessRating": 5,
    "qualityRating": 4,
    "quantityRating": 4,
    "varietyRating": 3,
    "totalRating": 4.0,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": null
  }
]
```

**Error Responses**:
- **401 Unauthorized**: Token expired or invalid

**Use Cases**:
- Show user's rating history in profile/settings
- Allow users to view/edit their previous ratings
- Display "My Ratings" section in user profile

---

### 4. Get Vendor Rating Summary

**Endpoint**: `GET /vendors/ratings/vendor/{vendorId}/summary`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `vendorId` (number, required): The ID of the vendor

**Success Response (200 OK)**:
```json
{
  "vendorId": 123,
  "averageRating": 4.25,
  "totalRatings": 8,
  "averageCollectionProcessRating": 4.5,
  "averageQualityRating": 4.3,
  "averageQuantityRating": 4.0,
  "averageVarietyRating": 4.2
}
```

**Response Fields**:
- `vendorId`: Vendor ID
- `averageRating`: Overall average rating (average of all total ratings)
- `totalRatings`: Total number of ratings submitted
- `averageCollectionProcessRating`: Average rating for collection process category
- `averageQualityRating`: Average rating for quality category
- `averageQuantityRating`: Average rating for quantity category
- `averageVarietyRating`: Average rating for variety category

**Error Responses**:
- **401 Unauthorized**: Token expired or invalid
- **500 Internal Server Error**: Server error

**Use Cases**:
- Display vendor rating summary on vendor profile
- Show category breakdown in rating details
- Display average ratings per category
- Show overall rating with star display

---

### 5. Check if an Order Has Been Rated

**Endpoint**: `GET /vendors/ratings/order/{orderId}/has-rated`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Path Parameters**:
- `orderId` (number, required): The completed order to check

**Success Response (200 OK)**:
```json
{
  "success": true,
  "message": "Order has been rated: true"
}
```

**Note**: Parse the message to extract the boolean value. The message format is "Order has been rated: true" or "Order has been rated: false".

**Error Responses**:
- **401 Unauthorized**: Token expired or invalid

**Use Cases**:
- After pickup, check whether to show the rating prompt for this specific order
- Determine if rating button should say "Rate Order" or "Update Rating"
- Prevent duplicate rating prompts for the same order

---

## Data Models

### RatingRequest (TypeScript/JavaScript)

```typescript
interface RatingRequest {
  vendorId: number;
  collectionProcessRating: number;  // 1-5
  qualityRating: number;             // 1-5
  quantityRating: number;            // 1-5
  varietyRating: number;             // 1-5
  orderId: number;                    // Required — completed order being rated
}
```

### RatingResponse (TypeScript/JavaScript)

```typescript
interface RatingResponse {
  id: number;
  vendorId: number;
  userId: number;
  orderId: number | null;
  collectionProcessRating: number;
  qualityRating: number;
  quantityRating: number;
  varietyRating: number;
  totalRating: number;
  createdAt: string;      // ISO 8601 format
  updatedAt: string | null; // ISO 8601 format or null
}
```

### VendorRatingSummary (TypeScript/JavaScript)

```typescript
interface VendorRatingSummary {
  vendorId: number;
  averageRating: number;
  totalRatings: number;
  averageCollectionProcessRating: number;
  averageQualityRating: number;
  averageQuantityRating: number;
  averageVarietyRating: number;
}
```

### ApiResponse (for has-rated endpoint)

```typescript
interface ApiResponse {
  success: boolean;
  message: string;
}
```

---

## UI/UX Implementation Guidelines

### Rating Submission Flow

1. **When to Prompt for Rating**:
   - After order completion (status = `COMPLETED`)
   - After successful pickup
   - From vendor profile page (if user hasn't rated yet)
   - From order history (for completed orders)
   - Consider showing a prompt 24 hours after order completion (not immediately)

2. **Rating Dialog/Screen**:
   - Create a rating dialog or full-screen rating activity
   - Display 4 rating categories with star ratings (1-5 stars each)
   - Categories with user-friendly labels:
     - **Collection Process**: "How was the pickup process?"
     - **Quality**: "How was the food quality?"
     - **Quantity**: "Was the quantity satisfactory?"
     - **Variety**: "Was there good variety?"
   - Each category should have a 5-star rating component
   - Show calculated total rating (average) as user selects stars
   - Include a "Submit" button (disabled until all 4 categories are rated)
   - Include a "Cancel" or "Skip" option
   - Show loading state while submitting

3. **Star Rating Component**:
   - Use a standard 5-star rating widget
   - Allow tapping on stars to set rating (1-5)
   - Visual feedback when stars are selected (filled vs empty)
   - Consider using filled/empty star icons
   - Show numeric value (e.g., "4 stars") for accessibility
   - Make stars large enough for easy tapping (minimum 40x40dp)

4. **Rating Display on Vendor Profile**:
   - Show average rating with star display: "4.5 ⭐"
   - Show total number of ratings: "(8 ratings)"
   - Optionally show category breakdown (if space allows)
   - Link to full ratings list/reviews
   - Show "Rate Vendor" button if user hasn't rated
   - Show "Update Rating" button if user has already rated

5. **Rating List/Reviews Screen**:
   - Display all ratings for a vendor in a scrollable list
   - Show each rating with:
     - User info (username or "Anonymous" - based on privacy settings)
     - Total rating (stars display)
     - Category breakdown (optional, can be expandable)
     - Date of rating (formatted nicely, e.g., "2 days ago")
   - Allow filtering/sorting by date, rating, etc.
   - Show "Load more" if there are many ratings

### Example React Native Components

```typescript
// Star Rating Component
import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';

interface StarRatingProps {
  rating: number;
  onRatingChange: (rating: number) => void;
  size?: number;
  disabled?: boolean;
}

export const StarRating: React.FC<StarRatingProps> = ({
  rating,
  onRatingChange,
  size = 32,
  disabled = false,
}) => {
  return (
    <View style={styles.container}>
      {[1, 2, 3, 4, 5].map((star) => (
        <TouchableOpacity
          key={star}
          onPress={() => !disabled && onRatingChange(star)}
          disabled={disabled}
          activeOpacity={0.7}
        >
          <Icon
            name={star <= rating ? 'star' : 'star-border'}
            size={size}
            color={star <= rating ? '#FFD700' : '#CCCCCC'}
          />
        </TouchableOpacity>
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
  },
});

// Rating Category Row
interface RatingCategoryRowProps {
  label: string;
  rating: number;
  onRatingChange: (rating: number) => void;
}

export const RatingCategoryRow: React.FC<RatingCategoryRowProps> = ({
  label,
  rating,
  onRatingChange,
}) => {
  return (
    <View style={categoryRowStyles.container}>
      <Text style={categoryRowStyles.label}>{label}</Text>
      <StarRating rating={rating} onRatingChange={onRatingChange} />
    </View>
  );
};

const categoryRowStyles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  label: {
    fontSize: 16,
    fontWeight: '500',
    flex: 1,
  },
});
```

### Rating Submission Screen Example

```typescript
import React, { useState } from 'react';
import { View, Text, Button, StyleSheet, ScrollView, ActivityIndicator } from 'react-native';
import { RatingCategoryRow } from './components/RatingCategoryRow';
import { submitRating } from './services/ratingService';

interface RatingSubmissionScreenProps {
  vendorId: number;
  orderId?: number;
  onRatingSubmitted: () => void;
  onDismiss: () => void;
}

export const RatingSubmissionScreen: React.FC<RatingSubmissionScreenProps> = ({
  vendorId,
  orderId,
  onRatingSubmitted,
  onDismiss,
}) => {
  const [collectionRating, setCollectionRating] = useState(0);
  const [qualityRating, setQualityRating] = useState(0);
  const [quantityRating, setQuantityRating] = useState(0);
  const [varietyRating, setVarietyRating] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const totalRating = 
    collectionRating > 0 && qualityRating > 0 && quantityRating > 0 && varietyRating > 0
      ? (collectionRating + qualityRating + quantityRating + varietyRating) / 4.0
      : 0;

  const canSubmit = collectionRating > 0 && qualityRating > 0 && quantityRating > 0 && varietyRating > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;

    setIsSubmitting(true);
    setError(null);

    try {
      await submitRating({
        vendorId,
        collectionProcessRating: collectionRating,
        qualityRating,
        quantityRating,
        varietyRating,
        orderId,
      });
      onRatingSubmitted();
    } catch (err: any) {
      setError(err.message || 'Failed to submit rating. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Rate Your Experience</Text>
      <Text style={styles.subtitle}>Help others by sharing your experience</Text>

      <RatingCategoryRow
        label="Collection Process"
        rating={collectionRating}
        onRatingChange={setCollectionRating}
      />
      <RatingCategoryRow
        label="Food Quality"
        rating={qualityRating}
        onRatingChange={setQualityRating}
      />
      <RatingCategoryRow
        label="Food Quantity"
        rating={quantityRating}
        onRatingChange={setQuantityRating}
      />
      <RatingCategoryRow
        label="Food Variety"
        rating={varietyRating}
        onRatingChange={setVarietyRating}
      />

      {totalRating > 0 && (
        <View style={styles.totalRatingContainer}>
          <Text style={styles.totalRatingLabel}>Overall Rating:</Text>
          <Text style={styles.totalRatingValue}>{totalRating.toFixed(1)} ⭐</Text>
        </View>
      )}

      {error && (
        <Text style={styles.errorText}>{error}</Text>
      )}

      <View style={styles.buttonContainer}>
        <Button
          title="Cancel"
          onPress={onDismiss}
          color="#666"
        />
        <Button
          title={isSubmitting ? "Submitting..." : "Submit Rating"}
          onPress={handleSubmit}
          disabled={!canSubmit || isSubmitting}
        />
      </View>

      {isSubmitting && (
        <ActivityIndicator size="large" style={styles.loader} />
      )}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 14,
    color: '#666',
    marginBottom: 24,
  },
  totalRatingContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 24,
    padding: 16,
    backgroundColor: '#F5F5F5',
    borderRadius: 8,
  },
  totalRatingLabel: {
    fontSize: 18,
    fontWeight: '600',
  },
  totalRatingValue: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#FFD700',
  },
  errorText: {
    color: '#FF0000',
    marginTop: 16,
    textAlign: 'center',
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginTop: 32,
    marginBottom: 16,
  },
  loader: {
    marginTop: 16,
  },
});
```

---

## API Service Implementation

### Rating Service (TypeScript/JavaScript)

```typescript
import { API_BASE_URL } from './config';

const RATING_ENDPOINTS = {
  submit: `${API_BASE_URL}/vendors/ratings/submit`,
  getByVendor: (vendorId: number) => `${API_BASE_URL}/vendors/ratings/vendor/${vendorId}`,
  getMyRatings: `${API_BASE_URL}/vendors/ratings/my-ratings`,
  getSummary: (vendorId: number) => `${API_BASE_URL}/vendors/ratings/vendor/${vendorId}/summary`,
  hasRated: (orderId: number) => `${API_BASE_URL}/vendors/ratings/order/${orderId}/has-rated`,
};

export const submitRating = async (request: RatingRequest): Promise<RatingResponse> => {
  const token = await getAuthToken(); // Your token retrieval method
  
  const response = await fetch(RATING_ENDPOINTS.submit, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    if (response.status === 401) {
      // Token expired - redirect to login
      throw new Error('Please log in to submit a rating.');
    }
    if (response.status === 400) {
      const error = await response.json();
      throw new Error(error.message || 'Invalid rating values. Please check your ratings.');
    }
    if (response.status === 404) {
      throw new Error('Vendor not found.');
    }
    throw new Error('Failed to submit rating. Please try again.');
  }

  return response.json();
};

export const getRatingsByVendor = async (vendorId: number): Promise<RatingResponse[]> => {
  const token = await getAuthToken();
  
  const response = await fetch(RATING_ENDPOINTS.getByVendor(vendorId), {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Please log in to view ratings.');
    }
    throw new Error('Failed to fetch ratings.');
  }

  return response.json();
};

export const getMyRatings = async (): Promise<RatingResponse[]> => {
  const token = await getAuthToken();
  
  const response = await fetch(RATING_ENDPOINTS.getMyRatings, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Please log in to view your ratings.');
    }
    throw new Error('Failed to fetch your ratings.');
  }

  return response.json();
};

export const getVendorRatingSummary = async (vendorId: number): Promise<VendorRatingSummary> => {
  const token = await getAuthToken();
  
  const response = await fetch(RATING_ENDPOINTS.getSummary(vendorId), {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Please log in to view rating summary.');
    }
    throw new Error('Failed to fetch rating summary.');
  }

  return response.json();
};

export const hasOrderBeenRated = async (orderId: number): Promise<boolean> => {
  const token = await getAuthToken();
  
  const response = await fetch(RATING_ENDPOINTS.hasRated(orderId), {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Please log in to check rating status.');
    }
    throw new Error('Failed to check rating status.');
  }

  const data: ApiResponse = await response.json();
  // Parse message: "Order has been rated: true" or "Order has been rated: false"
  return data.message.includes('true');
};
```

---

## Error Handling

### Common Error Scenarios

1. **Network Errors**:
   - Show retry button
   - Display user-friendly error message: "Failed to submit rating. Please check your connection and try again."
   - Allow user to retry without losing their rating selections

2. **Validation Errors (400)**:
   - Display specific validation messages
   - Highlight invalid fields
   - Example: "All ratings must be between 1 and 5 stars"
   - Don't clear user's selections, just show the error

3. **401 Unauthorized**:
   - Token expired - redirect to login screen
   - Show message: "Please log in to submit a rating"
   - Save rating state if possible (local storage) so user can submit after login

4. **404 Not Found**:
   - Vendor not found - show error and navigate back
   - Example: "Vendor not found. This vendor may no longer be available."

5. **Server Errors (500)**:
   - Show retry option
   - Log error for debugging
   - Message: "Something went wrong. Please try again later."
   - Don't lose user's rating selections

### Error Handling Example

```typescript
export const submitRatingWithErrorHandling = async (
  request: RatingRequest,
  onError: (error: string) => void,
  onSuccess: () => void
) => {
  try {
    await submitRating(request);
    onSuccess();
  } catch (error: any) {
    if (error.message.includes('log in') || error.message.includes('Unauthorized')) {
      // Redirect to login
      onError('Please log in to submit a rating.');
      // Navigate to login screen
    } else if (error.message.includes('Invalid') || error.message.includes('Validation')) {
      onError(error.message);
    } else if (error.message.includes('Network') || error.message.includes('connection')) {
      onError('Network error. Please check your connection and try again.');
    } else {
      onError('Failed to submit rating. Please try again later.');
    }
  }
};
```

---

## Integration Points

### 1. Order Completion Flow

After an order is completed, prompt the user to rate the vendor:

```typescript
// In OrderDetailsScreen or OrderCompletionScreen
useEffect(() => {
  const checkAndPromptRating = async () => {
    if (order.status === 'COMPLETED') {
      // Wait a bit before prompting (e.g., 2 seconds)
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      // Check if user has already rated
      try {
        const hasRated = await hasOrderBeenRated(order.id);
        if (!hasRated) {
          // Show rating prompt
          setShowRatingDialog(true);
        }
      } catch (error) {
        console.error('Error checking rating status:', error);
      }
    }
  };

  checkAndPromptRating();
}, [order.status, order.id]);
```

### 2. Vendor Profile Screen

- Display vendor's average rating and total ratings (from `getVendorRatingSummary`)
- Link to view all ratings/reviews (from `getRatingsByVendor`)
- Display category breakdown if available
- Per-order rating prompts happen on the order completion screen, not the vendor profile

```typescript
// In VendorProfileScreen
const [ratingSummary, setRatingSummary] = useState<VendorRatingSummary | null>(null);

useEffect(() => {
  const loadRatingInfo = async () => {
    try {
      const summary = await getVendorRatingSummary(vendorId);
      setRatingSummary(summary);
    } catch (error) {
      console.error('Error loading rating info:', error);
    }
  };

  loadRatingInfo();
}, [vendorId]);
```

### 3. Order History

- For completed orders, show rating status
- Allow rating from order history
- Show existing rating if already rated
- Display "Rate Vendor" button for completed orders that haven't been rated

### 4. User Profile/Settings

- Show "My Ratings" section
- Display all ratings the user has submitted (from `getMyRatings`)
- Allow users to view/edit their previous ratings
- Show vendor name and rating details for each rating

---

## Testing Checklist

- [ ] Submit rating with all 4 categories (1-5 stars each)
- [ ] Submit rating with optional orderId
- [ ] Submit rating without orderId
- [ ] Update existing rating (should update, not create duplicate)
- [ ] Validate rating values (reject values outside 1-5 range)
- [ ] Handle network errors gracefully (show retry option)
- [ ] Handle authentication errors (401) - redirect to login
- [ ] Handle vendor not found (404)
- [ ] Display vendor rating summary correctly
- [ ] Check if user has rated vendor (returns correct boolean)
- [ ] Display all ratings for a vendor
- [ ] Display user's own ratings
- [ ] Calculate total rating correctly (average of 4 categories)
- [ ] UI shows stars correctly (filled vs empty)
- [ ] Rating prompt appears after order completion
- [ ] Rating can be submitted from vendor profile
- [ ] Rating can be submitted from order history
- [ ] All 4 categories must be rated before submission
- [ ] Submit button is disabled until all categories are rated
- [ ] Loading state shows during submission
- [ ] Success message/confirmation after submission
- [ ] Error messages are user-friendly

---

## Additional Notes

1. **Rating Updates**: If a user has already rated a vendor, submitting a new rating will **update** the existing rating, not create a duplicate. The `updatedAt` field will be set.

2. **Vendor Average**: The vendor's average rating is automatically calculated and updated on the backend. You don't need to calculate it on the client. Use `getVendorRatingSummary` to get the latest average.

3. **Order ID**: The `orderId` field is optional but recommended. It helps track which order prompted the rating but doesn't affect uniqueness (one rating per user-vendor pair).

4. **Rating Display**: When displaying ratings, you can show:
   - Overall average: "4.5 ⭐"
   - Total count: "(8 ratings)"
   - Category breakdown: "Collection: 4.5, Quality: 4.3, Quantity: 4.0, Variety: 4.2"

5. **Privacy**: Consider showing usernames or "Anonymous" for ratings, depending on your privacy requirements. The API returns `userId` but you may want to fetch user details separately or show "Anonymous" to protect privacy.

6. **Timing**: Consider prompting users to rate 24 hours after order completion rather than immediately, to give them time to experience the food.

7. **Rate Limiting**: Be mindful of API rate limits. Cache rating summaries when possible and avoid excessive API calls.

---

## Summary

The rating system allows users to rate vendors with 4 categories (collection process, quality, quantity, variety), each rated 1-5 stars. The total rating is the average of these 4 categories. Ratings are **per completed order** — the same user can rate the same vendor on every pickup, and all ratings aggregate into the vendor average. Resubmitting for the same order updates that order's rating.

**Key Endpoints**:
- `POST /vendors/ratings/submit` - Submit or update a rating for a completed order
- `GET /vendors/ratings/vendor/{vendorId}` - Get all ratings for a vendor
- `GET /vendors/ratings/my-ratings` - Get current user's ratings
- `GET /vendors/ratings/vendor/{vendorId}/summary` - Get vendor rating summary
- `GET /vendors/ratings/order/{orderId}/has-rated` - Check if an order has been rated

All endpoints require authentication via JWT Bearer token and must be accessed through the API Gateway at `http://localhost:8080` (development).
