# Android Notification Integration Prompt

## Purpose

This prompt is for an AI agent implementing the full **push notification and in-app notification inbox** system in the StillFresh Android app. It covers:

1. Firebase Cloud Messaging (FCM) token registration and lifecycle
2. Receiving and handling push notifications in the foreground and background
3. Per-type deep-link routing from a push tap
4. In-app notification inbox (list, unread badge, mark-read, delete)
5. Notification preferences screen (enable/disable per type, enable/disable push globally)

---

## Architecture overview

```
Backend (notification-service)
  └─ Sends FCM "notification + data" hybrid messages to the user's registered token.

Android app
  └─ FirebaseMessagingService receives the message.
  └─ If app is in foreground: display a custom in-app notification or banner.
  └─ If app is in background/killed: OS shows the notification automatically
     (title + body from the "notification" part of the FCM message).
  └─ On notification tap: read the "data" map and navigate to the correct screen.
  └─ On login / token refresh: call POST /api/notifications/fcm-token/register.
  └─ On logout: call DELETE /api/notifications/fcm-token.
```

All API calls go through the API gateway at port `8080`. Use the app's existing base URL constant.

---

## Part 1 – FCM token lifecycle

### 1.1 Register token on login

After the user successfully logs in (JWT obtained), get the current FCM token and register it with the backend **before navigating to the home screen**.

```
POST {baseUrl}/api/notifications/fcm-token/register?token={fcmToken}
```

**Headers:**
```
Authorization: Bearer <jwt>
```

**No request body.** The token is passed as a query parameter.

**Success (200):**
```json
{
  "success": true,
  "message": "FCM token registered successfully",
  "data": { "userId": "123", "token": "<fcmToken>" },
  "error": null
}
```

**Implementation notes:**
- Use `FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> ... }` to obtain the token.
- If fetching the token fails, log the error and proceed with login anyway — FCM token registration must never block the login flow.

### 1.2 Refresh token (`onNewToken`)

In your `FirebaseMessagingService.onNewToken(token: String)` override, re-register the token if the user is currently logged in (JWT is present in SharedPreferences / DataStore).

```kotlin
override fun onNewToken(token: String) {
    super.onNewToken(token)
    val jwt = sessionManager.getToken() ?: return
    // Call POST /api/notifications/fcm-token/register?token=<token>
    notificationApiService.registerFcmToken(token)
}
```

### 1.3 Delete token on logout

When the user logs out, delete the registered FCM token from the backend so they stop receiving pushes.

```
DELETE {baseUrl}/api/notifications/fcm-token
```

**Headers:**
```
Authorization: Bearer <jwt>
```

**Success (200):**
```json
{ "success": true, "message": "FCM token deleted", "data": null, "error": null }
```

Call this **before** clearing the JWT from local storage. Failure is non-fatal; proceed with logout regardless.

---

## Part 2 – Receiving and displaying push notifications

### 2.1 FCM message structure

The backend sends **hybrid messages**: both a `notification` block (OS-rendered title/body) and a `data` map (for routing and context).

**Example FCM data map fields by notification type:**

| Type | Data keys always present | Additional data keys |
|---|---|---|
| `ORDER_CONFIRMED` (customer) | `orderId`, `offerId`, `quantity`, `totalPrice` | `name`, `location_name`, `address`, `zip_code`, `chain_name`, `image_url`, `vendor_image_url` |
| `ORDER_RECEIVED` (vendor) | `orderId`, `userId`, `offerId`, `quantity`, `totalPrice` | `offerName`, `name`, `location_name`, `address` |
| `ORDER_CANCELLED` | `orderId`, `offerId`, `cancelledBy`, `quantity`, `totalPrice` | `reason`, `name`, `address` |
| `ORDER_EXPIRED` | `orderId`, `offerId`, `quantity`, `totalPrice` | `name`, `address` |
| `ORDER_PICKUP_REMINDER` | `orderId`, `offerId`, `pickupBy` | `name`, `address` |
| `PAYMENT_SUCCESSFUL` | `requestId`, `offerId` | — |
| `PAYMENT_FAILED` | `requestId`, `offerId`, `reason` | — |
| `BANK_TRANSFER_INITIATED` | `reference`, `iban`, `bank_name`, `account_holder`, `amount`, `currency`, `expires_at`, `orderId` | `description` |
| `BANK_TRANSFER_CONFIRMED` | `orderId` | `reference` |
| `BANKING_MODEL_CHANGED` (vendor) | `chainId`, `newBankingModel`, `previousBankingModel`, `changedAt` | `chainName`, `headquartersEmail` |
| `SYSTEM_ALERT` | `test`, `timestamp` | — |

All data map values are **strings**, even numeric ones (e.g. `"quantity": "2"`, `"totalPrice": "12.50"`).

The notification type is **not** included in the data map. To identify the type when the app is tapped from background, use the `RemoteMessage.notification?.title` or store the type explicitly. **Recommended:** add a `type` key to your local notification builder (see 2.3 below) when constructing the notification in `onMessageReceived`.

### 2.2 Background and killed-state notifications

When the app is in the background or killed, the OS displays the notification automatically using the `notification.title` and `notification.body` from the FCM message. No code is needed for display. The `data` map is delivered to the launch `Intent` when the user taps the notification — read it in your launcher `Activity.onCreate` / `onNewIntent`.

```kotlin
// In Activity.onCreate or onNewIntent:
intent.extras?.let { extras ->
    val orderId = extras.getString("orderId")
    val type    = extras.getString("type") // if you added it yourself in onMessageReceived
    if (orderId != null) navigateToOrder(orderId)
}
```

### 2.3 Foreground notifications (`onMessageReceived`)

When the app is in the foreground, FCM does **not** show the notification automatically. Override `onMessageReceived` and build a `NotificationCompat` notification manually.

```kotlin
override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)

    val title = message.notification?.title ?: return
    val body  = message.notification?.body  ?: return
    val data  = message.data  // Map<String, String>

    val notificationId = System.currentTimeMillis().toInt()
    val pendingIntent  = buildDeepLinkIntent(data)

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(this).notify(notificationId, notification)
}
```

Create a **notification channel** in `Application.onCreate()`:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "StillFresh Notifications",
        NotificationManager.IMPORTANCE_HIGH
    ).apply { description = "Order and payment updates" }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
```

---

## Part 3 – Deep-link routing on notification tap

Build a helper that reads the `data` map and navigates to the correct screen:

| Notification type (inferred from data keys) | Navigation target |
|---|---|
| Has `orderId` and no `iban` | Order detail screen for `orderId` |
| Has `iban` / `reference` (bank transfer) | Show bank transfer instructions screen or open order detail |
| Has `requestId` + no `orderId` | Payment status / order detail via `requestId` |
| Has `chainId` (banking model changed) | Vendor banking settings screen |
| Has `test` key | No navigation (test notification) |

```kotlin
fun buildDeepLinkIntent(data: Map<String, String>): PendingIntent {
    val intent = when {
        data.containsKey("orderId") -> OrderDetailActivity.intent(context, data["orderId"]!!)
        data.containsKey("chainId") -> VendorBankingSettingsActivity.intent(context)
        else                        -> MainActivity.intent(context)
    }
    return PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
```

---

## Part 4 – In-app notification inbox

### 4.1 Notification model

```kotlin
data class AppNotification(
    val id: String,                  // UUID string
    val userId: String,
    val type: String,                // NotificationType enum name, e.g. "ORDER_CONFIRMED"
    val title: String?,
    val message: String?,
    val status: String,              // "PENDING", "SENT", "FAILED", "READ", "SKIPPED"
    val data: Map<String, String>?,  // parsed from JSON
    val isRead: Boolean,
    val deleted: Boolean,
    val createdAt: String,           // ISO-8601 OffsetDateTime string
    val sentAt: String?
)
```

The API wraps every response in:
```json
{
  "success": true,
  "message": "...",
  "data": <payload>,
  "error": null
}
```

### 4.2 Get all notifications (inbox)

```
GET {baseUrl}/api/notifications/user
Authorization: Bearer <jwt>
```

Returns a list of `AppNotification` sorted by `createdAt` descending. Only non-deleted notifications are returned.

### 4.3 Get unread notifications

```
GET {baseUrl}/api/notifications/user/unread
Authorization: Bearer <jwt>
```

Use the count of this response to drive a **badge** on the inbox icon in the bottom nav or toolbar.

### 4.4 Mark a single notification as read

```
POST {baseUrl}/api/notifications/mark-read/{notificationId}
Authorization: Bearer <jwt>
```

No request body. Call this when the user taps on a notification in the inbox list or opens the detail.

**Success (200):** `{ "success": true, "message": "Notification marked as read" }`

### 4.5 Mark all as read

```
POST {baseUrl}/api/notifications/mark-all-read
Authorization: Bearer <jwt>
```

Show a "Mark all as read" button in the inbox toolbar.

### 4.6 Delete a notification (soft delete)

```
DELETE {baseUrl}/api/notifications/{notificationId}
Authorization: Bearer <jwt>
```

Remove the item from the local list immediately on success. On 400 treat as "already gone". See `ANDROID_NOTIFICATION_DELETE_INTEGRATION_PROMPT.md` for full details.

---

## Part 5 – Notification preferences screen

### 5.1 Get current preferences

```
GET {baseUrl}/api/notifications/preferences
Authorization: Bearer <jwt>
```

**Response `data` field:**
```json
{
  "id": 1,
  "userId": "123",
  "pushEnabled": true,
  "emailEnabled": false,
  "smsEnabled": false,
  "enabledTypes": [
    "ORDER_CONFIRMED", "ORDER_RECEIVED", "ORDER_CANCELLED",
    "ORDER_EXPIRED", "ORDER_PICKUP_REMINDER",
    "PAYMENT_SUCCESSFUL", "PAYMENT_FAILED",
    "BANK_TRANSFER_INITIATED", "BANK_TRANSFER_CONFIRMED",
    "BANKING_MODEL_CHANGED", "SYSTEM_ALERT"
  ],
  "createdAt": "2025-01-01T10:00:00Z",
  "updatedAt": "2025-06-01T12:00:00Z"
}
```

If the user has never saved preferences, the backend returns `"data": null` — in this case treat everything as enabled by default.

### 5.2 Update preferences

```
POST {baseUrl}/api/notifications/preferences
Authorization: Bearer <jwt>
Content-Type: application/json
```

**Request body:**
```json
{
  "pushEnabled": true,
  "emailEnabled": false,
  "smsEnabled": false,
  "enabledTypes": [
    "ORDER_CONFIRMED",
    "ORDER_RECEIVED",
    "ORDER_CANCELLED",
    "ORDER_EXPIRED",
    "ORDER_PICKUP_REMINDER",
    "PAYMENT_SUCCESSFUL",
    "PAYMENT_FAILED",
    "BANK_TRANSFER_INITIATED",
    "BANK_TRANSFER_CONFIRMED",
    "BANKING_MODEL_CHANGED",
    "SYSTEM_ALERT"
  ]
}
```

`enabledTypes` must be a JSON array of strings matching the `NotificationType` enum values exactly. Send the full desired set — it replaces the previous set entirely.

### 5.3 Preferences screen UI

Build a dedicated "Notification Settings" screen accessible from the user/account settings. Recommended layout:

**Master toggle:**
- "Push notifications" — maps to `pushEnabled`. When OFF, all push is suppressed server-side regardless of individual type toggles; grey out the individual toggles.

**Per-type toggles** (only show types relevant to the user's role):

*For customer accounts:*
| Label | `NotificationType` value |
|---|---|
| Order confirmed | `ORDER_CONFIRMED` |
| Order cancelled / rejected | `ORDER_CANCELLED` |
| Order expired | `ORDER_EXPIRED` |
| Pickup reminder | `ORDER_PICKUP_REMINDER` |
| Payment successful | `PAYMENT_SUCCESSFUL` |
| Payment failed | `PAYMENT_FAILED` |
| Bank transfer instructions | `BANK_TRANSFER_INITIATED` |
| Bank transfer confirmed | `BANK_TRANSFER_CONFIRMED` |

*For vendor accounts:*
| Label | `NotificationType` value |
|---|---|
| New order received | `ORDER_RECEIVED` |
| Order cancelled by customer | `ORDER_CANCELLED` |
| Banking model changed | `BANKING_MODEL_CHANGED` |

Save on change (debounce or on-back), not requiring a separate "Save" button. Show a brief Snackbar on error.

---

## Part 6 – Unread badge

- After login and on app resume, call `GET /api/notifications/user/unread` and display the count on the inbox icon.
- When the user opens the inbox, refresh the unread count (or set it to 0 if you mark-all-read).
- When a foreground FCM message is received, increment the badge count by 1 (or re-fetch the unread count).

---

## Part 7 – Error handling

| HTTP status | Action |
|---|---|
| 200 | Success — update UI |
| 400 | Show inline error message; for delete treat as "already gone" |
| 401 | Token expired — trigger token refresh or redirect to login |
| 500 / network error | Show Snackbar "Something went wrong, please try again"; do not mutate local state |

---

## Checklist for the AI agent

**FCM lifecycle:**
- [ ] `POST /api/notifications/fcm-token/register?token=...` called after login with current FCM token
- [ ] `onNewToken` override re-registers token if user is logged in
- [ ] `DELETE /api/notifications/fcm-token` called on logout (before clearing JWT)

**Foreground push:**
- [ ] `onMessageReceived` builds and shows a `NotificationCompat` notification
- [ ] Notification channel created in `Application.onCreate()` for API 26+
- [ ] `PendingIntent` routes to the correct screen based on `data` map keys

**Background / killed-state tap:**
- [ ] Launch `Activity` reads `intent.extras` and routes to correct screen

**Inbox:**
- [ ] `GET /api/notifications/user` powers the inbox list
- [ ] Unread badge from `GET /api/notifications/user/unread`
- [ ] Tapping a notification calls `POST /api/notifications/mark-read/{id}`
- [ ] "Mark all as read" button calls `POST /api/notifications/mark-all-read`
- [ ] Swipe/delete calls `DELETE /api/notifications/{id}` and removes from list

**Preferences:**
- [ ] `GET /api/notifications/preferences` loads the preferences screen
- [ ] Master push toggle maps to `pushEnabled`; individual toggles map to `enabledTypes` entries
- [ ] `POST /api/notifications/preferences` saves on change
- [ ] Only show type toggles relevant to the user's role (customer vs vendor)

---

## Backend context (for reference)

- The backend sends one FCM token per user (single device). When a new token is registered, it replaces the previous one.
- The backend persists every notification in the `notifications` table regardless of whether push was delivered (push may be `SKIPPED` if the user disabled it in preferences). The in-app inbox always shows the full history.
- Notifications older than 90 days are hard-deleted by a scheduled job.
- `emailEnabled` and `smsEnabled` preferences are stored but the email channel is controlled server-side by `notification.email.enabled`; do not surface email/SMS toggles unless the product explicitly supports them.

Use this document as the single source of truth for all notification integration in the StillFresh Android app.
