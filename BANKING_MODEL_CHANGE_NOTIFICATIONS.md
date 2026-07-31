# Banking Model Change Notification System

## Overview

This document describes the comprehensive notification system implemented for banking model changes in chain restaurants. When a `VENDOR_ADMIN` switches between **SHARED** and **INDIVIDUAL** banking models, all chain locations are notified in real-time through multiple channels.

## Architecture

### Components

1. **Vendor Service** (`vendor-service`)
   - Handles banking model switching logic
   - Publishes `BankingModelChangedEvent` to Kafka
   - Sends email notifications to all location managers

2. **Notification Service** (`notification-service`)
   - Consumes `BankingModelChangedEvent` from Kafka
   - Sends in-app/push notifications to all chain locations

3. **Shared Entities** (`shared-entities`)
   - `BankingModelChangedEvent`: Kafka event for banking model changes
   - `NotificationType.BANKING_MODEL_CHANGED`: Notification type enum

## Notification Channels

### 1. Email Notifications (Immediate)

**When:** Banking model is switched  
**Recipients:** All location managers (except the one who made the change)  
**Content:**
- Clear explanation of what changed
- Impact on payment routing
- Who made the change
- Effective date (immediate)

**Email Templates:**

**SHARED Model:**
```
Dear [Chain Name] Location Manager,

The banking model for your chain '[Chain Name]' has been changed to SHARED.

What this means:
- All locations in your chain will now use the headquarters payment account.
- Payments from all locations will be routed to the headquarters Stripe account.
- You no longer need to manage individual payment accounts for this location.

Changed by: [Email]
Headquarters: [Location Name] ([Email])
Effective immediately.

If you have any questions, please contact your chain administrator.

Best regards,
StillFresh Team
```

**INDIVIDUAL Model:**
```
Dear [Chain Name] Location Manager,

The banking model for your chain '[Chain Name]' has been changed to INDIVIDUAL.

What this means:
- Each location now uses its own payment account.
- Payments from your location will be routed to your individual Stripe account.
- You are responsible for managing your own payment account.

IMPORTANT - OFFER INVALIDATION:
All active offers for your location have been automatically invalidated.
This is required because your location must have its own payment account configured
before offers can be active. Once you have set up your individual payment account,
you can reactivate your offers through the vendor dashboard.

Next Steps:
1. Set up your individual payment account (if not already done)
2. Verify your payment account is ready to receive payments
3. Reactivate your offers through the vendor dashboard

Changed by: [Email]
Effective immediately.

If you have any questions, please contact your chain administrator.

Best regards,
StillFresh Team
```

### 2. Kafka Event (Real-time)

**Event:** `BankingModelChangedEvent`  
**Topic:** `banking-model-changed` (configurable via `vendor.topic.banking-model-changed`)  
**Payload:**
```json
{
  "chainId": "uuid",
  "chainName": "Chain Name",
  "newBankingModel": "SHARED" | "INDIVIDUAL",
  "previousBankingModel": "SHARED" | "INDIVIDUAL",
  "changedByVendorId": 123,
  "changedByEmail": "admin@example.com",
  "headquartersVendorId": 456,
  "headquartersEmail": "hq@example.com",
  "locationVendorIds": [123, 456, 789],
  "changedAt": "2024-01-01T12:00:00Z"
}
```

### 3. In-App/Push Notifications (Real-time)

**When:** `BankingModelChangedEvent` is consumed by notification-service  
**Recipients:** All chain locations (except the one who made the change)  
**Type:** `NotificationType.BANKING_MODEL_CHANGED`  
**Delivery:** 
- Push notifications via FCM (if mobile app is installed)
- In-app notifications (if user is logged in)
- Notification history in user's notification center

**Notification Content:**
- **Title:** "Banking Model Changed to SHARED" or "Banking Model Changed to INDIVIDUAL"
- **Message:** Contextual message explaining the change and its impact
  - For INDIVIDUAL: Includes information about offer invalidation and next steps
- **Data:** Includes chain ID, model details, change timestamp, etc.

## Flow Diagram

```
┌─────────────────┐
│  VENDOR_ADMIN   │
│  Switches Model │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│  VendorService          │
│  switchBankingModel()   │
└────────┬────────────────┘
         │
         ├──► Update Database (all locations)
         │
         ├──► Publish BankingModelChangedEvent (Kafka)
         │
         └──► Send Email Notifications (non-blocking)
              │
              ├──► Location 1 Manager
              ├──► Location 2 Manager
              └──► Location N Manager

         ▼
┌─────────────────────────┐
│  Kafka Topic            │
│  banking-model-changed  │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│  BankingModelChanged     │
│  Consumer                │
│  (notification-service) │
└────────┬────────────────┘
         │
         └──► Send In-App/Push Notifications
              │
              ├──► Location 1 (FCM + In-App)
              ├──► Location 2 (FCM + In-App)
              └──► Location N (FCM + In-App)
```

## Offer Invalidation (SHARED → INDIVIDUAL)

When switching from **SHARED** to **INDIVIDUAL** banking model:

1. **Automatic Offer Invalidation:**
   - All active offers from all chain locations are automatically invalidated
   - Reason: Each location must have its own payment account configured before offers can be active
   - Prevents payment routing issues during the transition

2. **Vendor Action Required:**
   - Location managers must set up their individual payment accounts
   - Once payment account is configured and ready, offers can be reactivated
   - Offers remain invalidated until payment account is properly set up

3. **Notification:**
   - Email and in-app notifications clearly explain why offers were invalidated
   - Provides clear next steps for reactivating offers

## Best Practices Implemented

### 1. **Multi-Channel Notification**
- **Email:** Reliable, persistent record, works even if app is not installed
- **Kafka Event:** Real-time, decoupled, scalable
- **In-App/Push:** Immediate visibility, better UX

### 2. **Non-Blocking Operations**
- Email sending is wrapped in try-catch blocks
- If email fails, the banking model change still succeeds
- Errors are logged but don't block the operation

### 3. **Idempotency**
- Checks if banking model is already set to the requested value
- Skips unnecessary operations if no change is needed

### 4. **Comprehensive Audit Trail**
- All changes are logged with:
  - Who made the change
  - When it was made
  - What changed (previous → new)
  - Which locations were affected

### 5. **Validation Before Change**
- **SHARED → INDIVIDUAL:** Validates all locations have payment accounts
- **INDIVIDUAL → SHARED:** Validates headquarters has payment account
- Prevents invalid state transitions

### 6. **Real-Time Updates**
- Kafka events ensure immediate propagation
- Notification service processes events asynchronously
- No polling or delay

### 7. **Offer Invalidation on Model Change**
- When switching to INDIVIDUAL, all offers are automatically invalidated
- Prevents payment routing issues during transition
- Clear communication about why offers were invalidated
- Guidance on reactivating offers after payment account setup

## Configuration

### Kafka Topics

```properties
# Vendor Service
vendor.topic.banking-model-changed=banking-model-changed
```

### Email Configuration

Email service uses existing Mailgun configuration:
```properties
mailgun.api.key=your-api-key
mailgun.domain=your-domain
mailgun.from.email=noreply@yourdomain.com
```

## Error Handling

### Email Failures
- **Behavior:** Logged as warnings, operation continues
- **Reason:** Email is supplementary, not critical for operation
- **Recovery:** Email can be resent manually if needed

### Kafka Event Failures
- **Behavior:** Logged as errors, operation continues
- **Reason:** Database update is the source of truth
- **Recovery:** Event can be republished if needed

### Notification Service Failures
- **Behavior:** Logged as errors in notification-service
- **Reason:** Notifications are supplementary
- **Recovery:** Events are retried by Kafka consumer

## Testing Recommendations

1. **Unit Tests:**
   - Test email template generation
   - Test event creation with correct location IDs
   - Test validation logic

2. **Integration Tests:**
   - Test Kafka event publishing
   - Test notification service consumption
   - Test email delivery

3. **End-to-End Tests:**
   - Switch banking model
   - Verify emails are received
   - Verify in-app notifications appear
   - Verify database updates

## Future Enhancements

1. **Notification Preferences:**
   - Allow location managers to opt-in/opt-out of email notifications
   - Allow preference for email vs. in-app notifications

2. **Notification History:**
   - Store notification history in database
   - Allow users to view past notifications

3. **Webhook Support:**
   - Allow external systems to subscribe to banking model changes
   - Webhook notifications for third-party integrations

4. **SMS Notifications:**
   - Add SMS as an additional notification channel
   - Critical for urgent changes

5. **Confirmation Flow:**
   - Require confirmation from location managers before switching
   - Two-step verification for critical changes

## Security Considerations

1. **Authorization:**
   - Only `VENDOR_ADMIN` can switch banking models
   - Validated at service level

2. **Audit Logging:**
   - All changes are logged with user context
   - Timestamps and user IDs are recorded

3. **Data Privacy:**
   - Email addresses are only used for notifications
   - No sensitive payment data in notifications

## Monitoring

### Key Metrics to Monitor

1. **Email Delivery Rate:**
   - Track successful vs. failed email sends
   - Alert on high failure rates

2. **Kafka Event Processing:**
   - Monitor event publishing success rate
   - Monitor consumer lag

3. **Notification Delivery:**
   - Track in-app notification delivery rate
   - Track push notification delivery rate

4. **Banking Model Changes:**
   - Track frequency of changes
   - Alert on unusual patterns

## Login Response Enhancement

The login response now includes `usesSharedPaymentAccount` field for vendors:

```json
{
  "jwt": "token...",
  "role": "VENDOR_ADMIN",
  "vendor": {
    "id": 123,
    "email": "vendor@example.com",
    "isHeadquarters": true,
    "isChainLocation": true,
    "isUniqueVendor": false,
    "chainName": "Chain Name",
    "locationName": "Headquarters",
    "usesSharedPaymentAccount": true
  }
}
```

This allows the frontend/mobile app to:
- Display appropriate payment account information
- Show correct banking model status
- Guide users through payment account setup if needed

## Summary

The banking model change notification system provides:
- ✅ **Real-time notifications** via Kafka events
- ✅ **Email notifications** for persistent record
- ✅ **In-app/push notifications** for immediate visibility
- ✅ **Non-blocking operations** for reliability
- ✅ **Comprehensive audit trail** for compliance
- ✅ **Multi-channel delivery** for maximum reach
- ✅ **Automatic offer invalidation** when switching to INDIVIDUAL
- ✅ **Clear guidance** on reactivating offers after payment setup
- ✅ **Login response enhancement** with `usesSharedPaymentAccount` field

This ensures all chain locations are immediately aware of banking model changes, understand why offers were invalidated (if applicable), and know exactly what steps to take next.

