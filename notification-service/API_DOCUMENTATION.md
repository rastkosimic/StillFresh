# Notification Service API Documentation

## Overview
All endpoints now require JWT authentication via the `Authorization` header. The `userId` is automatically extracted from the JWT token, making the API more secure and user-specific. All responses are returned in a standardized JSON format.

## Authentication
All requests must include the JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Response Format
All API responses follow this standardized JSON structure:
```json
{
  "success": true/false,
  "message": "Human-readable message",
  "data": { /* Response data or null */ },
  "error": "Error details (only present on failure)"
}
```

## API Endpoints

### 1. FCM Token Management

#### Register FCM Token
```http
POST /api/notifications/fcm-token/register
Authorization: Bearer <jwt-token>
Content-Type: application/x-www-form-urlencoded

token=your_fcm_token_here
```

**Response:**
```json
{
  "success": true,
  "message": "FCM token registered successfully",
  "data": {
    "id": "uuid",
    "userId": "user123",
    "token": "fcm_token_here",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  "error": null
}
```

#### Get FCM Token
```http
GET /api/notifications/fcm-token
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "FCM token retrieved successfully",
  "data": {
    "id": "uuid",
    "userId": "user123",
    "token": "fcm_token_here",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  "error": null
}
```

#### Delete FCM Token
```http
DELETE /api/notifications/fcm-token
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "FCM token deleted successfully",
  "data": null,
  "error": null
}
```

### 2. Notification Management

#### Get User Notifications
```http
GET /api/notifications/user
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Notifications retrieved successfully",
  "data": [
    {
      "id": "uuid",
      "userId": "user123",
      "type": "ORDER_CONFIRMED",
      "title": "Order Confirmed",
      "message": "Your order has been confirmed!",
      "status": "SENT",
      "data": {
        "orderId": "12345",
        "totalPrice": "25.99"
      },
      "createdAt": "2024-01-15T10:30:00Z",
      "sentAt": "2024-01-15T10:30:05Z"
    }
  ],
  "error": null
}
```

#### Mark Notification as Read
```http
POST /api/notifications/mark-read/{notificationId}
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": null,
  "error": null
}
```

#### Mark All Notifications as Read
```http
POST /api/notifications/mark-all-read
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "All notifications marked as read",
  "data": null,
  "error": null
}
```

### 3. Notification Preferences

#### Get User Preferences
```http
GET /api/notifications/preferences
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Preferences retrieved successfully",
  "data": {
    "id": 1,
    "userId": "user123",
    "enabledTypes": ["ORDER_CONFIRMED", "PAYMENT_SUCCESSFUL"],
    "pushEnabled": true,
    "emailEnabled": false,
    "smsEnabled": false,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  "error": null
}
```

#### Update User Preferences
```http
POST /api/notifications/preferences
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "enabledTypes": ["ORDER_CONFIRMED", "PAYMENT_SUCCESSFUL", "OFFER_AVAILABLE"],
  "pushEnabled": true,
  "emailEnabled": false,
  "smsEnabled": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "Preferences updated successfully",
  "data": {
    "id": 1,
    "userId": "user123",
    "enabledTypes": ["ORDER_CONFIRMED", "PAYMENT_SUCCESSFUL", "OFFER_AVAILABLE"],
    "pushEnabled": true,
    "emailEnabled": false,
    "smsEnabled": false,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:35:00Z"
  },
  "error": null
}
```

### 4. Test Notifications

#### Send Test Notification
```http
POST /api/notifications/test
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Test notification sent",
  "data": null,
  "error": null
}
```

## Error Responses

### Invalid JWT Token
```json
{
  "success": false,
  "message": null,
  "data": null,
  "error": "Invalid JWT token: <error-message>"
}
```

### Notification Not Found or Access Denied
```json
{
  "success": false,
  "message": null,
  "data": null,
  "error": "Notification not found or access denied"
}
```

### FCM Token Not Found
```json
{
  "success": false,
  "message": null,
  "data": null,
  "error": "FCM token not found"
}
```

## Security Features

1. **JWT Authentication**: All endpoints require valid JWT tokens
2. **User Isolation**: Users can only access their own data
3. **Notification Ownership**: Users can only mark their own notifications as read
4. **Automatic User ID Extraction**: No need to pass userId in requests

## Migration from Old API

### Before (Old API):
```http
GET /api/notifications/user/123
POST /api/notifications/fcm-token/register?userId=123&token=abc
```

### After (New API):
```http
GET /api/notifications/user
Authorization: Bearer <jwt-token>

POST /api/notifications/fcm-token/register?token=abc
Authorization: Bearer <jwt-token>
```

## Mobile App Integration

### Android Example:
```kotlin
// Register FCM token
val call = apiService.registerFcmToken(fcmToken)
call.enqueue(object : Callback<String> {
    override fun onResponse(call: Call<String>, response: Response<String>) {
        // Handle success
    }
    override fun onFailure(call: Call<String>, t: Throwable) {
        // Handle error
    }
})

// Get notifications
val call = apiService.getUserNotifications()
call.enqueue(object : Callback<List<Notification>> {
    override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
        // Handle notifications
    }
    override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
        // Handle error
    }
})
```

### iOS Example:
```swift
// Register FCM token
func registerFcmToken(token: String) {
    let url = URL(string: "\(baseURL)/api/notifications/fcm-token/register")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("Bearer \(jwtToken)", forHTTPHeaderField: "Authorization")
    request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
    
    let body = "token=\(token)"
    request.httpBody = body.data(using: .utf8)
    
    URLSession.shared.dataTask(with: request) { data, response, error in
        // Handle response
    }.resume()
}
```

## Benefits of JWT-based Authentication

1. **Security**: No userId in URL or request body
2. **Stateless**: No need to maintain session state
3. **Scalable**: Works across multiple service instances
4. **User Context**: Automatic user identification
5. **Authorization**: Built-in user permissions
6. **Audit Trail**: JWT contains user information for logging


