# Notification Service

The Notification Service is a microservice responsible for handling push notifications in the StillFresh application. It integrates with Firebase Cloud Messaging (FCM) to send notifications to mobile devices and provides a comprehensive API for notification management.

## Features

- **Push Notifications**: Send notifications to mobile devices using Firebase Cloud Messaging
- **Event-Driven Architecture**: Listens to Kafka events from other microservices
- **Notification Types**: Support for various notification types (orders, payments, offers, etc.)
- **User Preferences**: Allow users to customize notification settings
- **Retry Logic**: Automatic retry for failed notifications
- **Metrics & Monitoring**: Built-in metrics for monitoring notification performance
- **FCM Token Management**: Register and manage FCM tokens for users

## Architecture

### Event Flow

1. **Order Placed**: Order service publishes `OrderPlacedEvent` → Notification service sends confirmation to user
2. **Vendor Notification**: Order service publishes `VendorOrderNotificationEvent` → Notification service notifies vendor
3. **Payment Events**: Payment service publishes success/failure events → Notification service sends payment notifications
4. **Offer Events**: Offer service publishes creation/update events → Notification service can notify interested users

### Components

- **Consumers**: Listen to Kafka events from other services
- **Services**: Handle business logic for notifications and preferences
- **Controllers**: REST API endpoints for mobile app integration
- **Repositories**: Data access layer for notifications and preferences
- **Metrics**: Monitoring and observability

## API Response Format

All endpoints return standardized JSON responses with the following structure:

### Success Response
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { /* Response data or null */ },
  "error": null
}
```

### Error Response
```json
{
  "success": false,
  "message": null,
  "data": null,
  "error": "Detailed error information"
}
```

### Response Fields
- **success**: Boolean indicating if the operation was successful
- **message**: Human-readable message describing the operation result
- **data**: The actual response data (can be null for operations that don't return data)
- **error**: Error details (only present when success is false)

## API Endpoints

### FCM Token Management
- `POST /api/notifications/fcm-token/register` - Register FCM token for user
- `GET /api/notifications/fcm-token` - Get FCM token for user
- `DELETE /api/notifications/fcm-token` - Remove FCM token for user

### Notification Management
- `GET /api/notifications/user` - Get user notifications
- `POST /api/notifications/mark-read/{notificationId}` - Mark notification as read
- `POST /api/notifications/mark-all-read` - Mark all notifications as read
- `POST /api/notifications/test` - Send test notification

### Notification Preferences
- `GET /api/notifications/preferences` - Get user preferences
- `POST /api/notifications/preferences` - Update user preferences

**Note**: All endpoints now use JWT authentication. The `userId` is automatically extracted from the JWT token, so it's no longer required in the URL path.

## Configuration

### Application Properties

```yaml
server:
  port: 8087

spring:
  application:
    name: notification-service
  datasource:
    url: jdbc:postgresql://notification-postgres:5432/stillfresh_notificationdb
    username: stillfreshnotification
    password: ${POSTGRES_PASSWORD_NOTIFICATION:}
  kafka:
    bootstrap-servers: ${KAFKA_BROKER:kafka:9092}
  data:
    redis:
      host: redis
      port: 6379

firebase:
  credentials-path: ${FIREBASE_CREDENTIALS_PATH:/app/firebase/firebase-service-account.json}
  project-id: stillfresh-app

notification:
  topics:
    notification-request: notification-request
    notification-sent: notification-sent
    notification-failed: notification-failed

order:
  topic:
    order-placed: order-placed
    vendor-order-notification: vendor-order-notification

payment:
  topic:
    payment-success: payment-success
    payment-failure: payment-failure

offer:
  topic:
    offer-created: offer-created
    offer-updated: offer-updated
```

## Database Schema

### Tables

1. **notifications**: Stores notification records
2. **fcm_tokens**: Stores FCM tokens for users
3. **notification_preferences**: Stores user notification preferences
4. **enabled_notification_types**: Stores enabled notification types per user

### Key Fields

- `notifications`: id, user_id, type, title, message, status, data, created_at, sent_at
- `fcm_tokens`: id, user_id, token, created_at, updated_at
- `notification_preferences`: id, user_id, push_enabled, email_enabled, sms_enabled

## Notification Types

- `ORDER_CONFIRMED`: Order has been confirmed
- `ORDER_RECEIVED`: New order received (for vendors)
- `ORDER_READY`: Order is ready for pickup
- `ORDER_CANCELLED`: Order has been cancelled
- `PAYMENT_SUCCESSFUL`: Payment processed successfully
- `PAYMENT_FAILED`: Payment failed
- `OFFER_AVAILABLE`: New offer available
- `OFFER_EXPIRING`: Offer is about to expire
- `ACCOUNT_VERIFIED`: Account has been verified
- `SYSTEM_ALERT`: System notifications

## Monitoring

### Metrics

- `notifications.sent`: Number of notifications sent successfully
- `notifications.failed`: Number of notifications that failed
- `notifications.received`: Number of notifications received
- `fcm.tokens.registered`: Number of FCM tokens registered

### Health Checks

- Database connectivity
- Kafka connectivity
- Redis connectivity
- Firebase connectivity

## Development

### Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+
- Kafka 2.8+
- Firebase project with FCM enabled

### Running the Service

1. Start dependencies (PostgreSQL, Redis, Kafka)
2. Configure Firebase credentials
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Testing

```bash
mvn test
```

## Integration with Mobile Apps

### Android Integration

1. Register FCM token:
   ```kotlin
   POST /api/notifications/fcm-token/register
   {
     "userId": "user123",
     "token": "fcm_token_here"
   }
   ```

2. Handle incoming notifications:
   ```kotlin
   // In FirebaseMessagingService
   override fun onMessageReceived(remoteMessage: RemoteMessage) {
       // Handle notification data
       val orderId = remoteMessage.data["orderId"]
       val type = remoteMessage.data["type"]
   }
   ```

### iOS Integration

1. Register FCM token:
   ```swift
   POST /api/notifications/fcm-token/register
   {
     "userId": "user123",
     "token": "fcm_token_here"
   }
   ```

2. Handle incoming notifications:
   ```swift
   // In AppDelegate
   func userNotificationCenter(_ center: UNUserNotificationCenter,
                              didReceive response: UNNotificationResponse,
                              withCompletionHandler completionHandler: @escaping () -> Void) {
       // Handle notification data
       let orderId = response.notification.request.content.userInfo["orderId"]
       let type = response.notification.request.content.userInfo["type"]
   }
   ```

## Troubleshooting

### Common Issues

1. **FCM Token Not Found**: Ensure user has registered their FCM token
2. **Notification Not Sent**: Check Firebase credentials and network connectivity
3. **Kafka Consumer Issues**: Verify topic names and consumer group configuration
4. **Database Connection**: Check PostgreSQL connection and credentials

### Logs

- Application logs: Check for error messages and stack traces
- Kafka logs: Monitor consumer lag and processing errors
- Firebase logs: Check FCM delivery reports

## Security

- FCM tokens are stored securely in the database
- User preferences are user-specific and isolated
- API endpoints require proper authentication (integrate with your auth service)
- Sensitive data in notifications should be minimal

## Performance Considerations

- Use Redis for caching frequently accessed data
- Implement proper indexing on database tables
- Monitor notification queue size and processing time
- Consider rate limiting for high-volume scenarios
- Use batch processing for bulk notifications








