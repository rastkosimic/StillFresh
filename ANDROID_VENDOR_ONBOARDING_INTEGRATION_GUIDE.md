# Android Vendor Onboarding Integration Guide

## Overview

This guide describes the complete vendor onboarding process for the StillFresh Android application. The onboarding system supports both **unique vendors** (single location) and **chain vendors** (multiple locations), with flexible banking models and worker management.

## Table of Contents

1. [Onboarding Flow Overview](#onboarding-flow-overview)
2. [Authentication & Roles](#authentication--roles)
3. [Phase 1: Vendor Application (Public)](#phase-1-vendor-application-public)
4. [Phase 1b: Admin Review & Activation (Platform Admin)](#phase-1b-admin-review--activation-platform-admin)
5. [Phase 2: Vendor Onboarding Steps](#phase-2-vendor-onboarding-steps)
6. [Phase 3: Chain Location Management](#phase-3-chain-location-management)
7. [Phase 4: Banking Model Management](#phase-4-banking-model-management)
8. [Phase 5: Worker Management](#phase-5-worker-management)
9. [API Endpoints Reference](#api-endpoints-reference)
10. [Error Handling](#error-handling)
11. [UI/UX Recommendations](#uiux-recommendations)

---

## Onboarding Flow Overview

The vendor onboarding process follows these stages:

```
1. Vendor Application (Public - No Auth Required)
   ↓
2. Admin Review & Verification (Platform Admin)
   ↓
3. Vendor Receives Credentials & Logs In
   ↓
4. Vendor Type Selection (VENDOR_ADMIN)
   ↓
5. Headquarters Setup (CHAIN only)
   ↓
6. Banking Model Selection
   ↓
7. Payment Account Setup
   ↓
8. Onboarding Complete
   ↓
9. Chain Location Management (Optional, CHAIN only)
   ↓
10. Worker Management (Optional)
```

### Onboarding Status Enum

```kotlin
enum class OnboardingStatus {
    PENDING_VERIFICATION,    // Initial registration, awaiting admin verification
    VERIFIED,                // Admin verified, ready for onboarding steps
    TYPE_SELECTED,           // Vendor type (CHAIN/UNIQUE) selected
    HEADQUARTERS_ADDED,      // HQ added (CHAIN only)
    BANKING_SETUP,           // Banking model selected
    PAYMENT_CONFIGURED,      // Payment account ready
    COMPLETED                // Full onboarding done
}
```

---

## Authentication & Roles

### Roles Hierarchy

- **SUPER_ADMIN**: Business owner, can manage all roles
- **ADMIN**: Platform admin, can manage VENDOR_ADMIN and VENDOR roles
- **VENDOR_ADMIN**: Chain/location owner, can manage VENDOR workers
- **VENDOR**: Worker, can only manage offers for assigned location

### Authentication Flow

1. Vendor receives credentials via email after admin verification
2. Login using email/username and password
3. JWT token contains role information
4. All subsequent requests include JWT in `Authorization: Bearer <token>` header

---

## Phase 1: Vendor Application (Public)

**Note**: This phase allows potential vendors to submit applications. No authentication required.

### Step 1: Submit Vendor Application

**Endpoint**: `POST /vendors/apply`

**Authorization**: None required (public endpoint)

**Request Body**:
```json
{
  "contactName": "John Doe",
  "email": "vendor@example.com",
  "phone": "+1234567890",
  "businessAddress": "123 Main St",
  "zipCode": "12345",
  "businessRegistrationId": "REG123456",  // Optional
  "notes": "Additional information about the business"  // Optional
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Vendor application submitted successfully. Your application will be reviewed by our team. You will receive credentials via email once your application is approved."
}
```

**Error Responses**:
- `400 Bad Request`: Validation failed
- `409 Conflict`: Email already exists
- `500 Internal Server Error`: Server error

**Important Notes**:
- **Coordinates (latitude/longitude) are NOT required** at this stage
- Default coordinates (0.0, 0.0) are set automatically
- Coordinates will be set during the onboarding process after vendor receives credentials
- Vendor status will be `PENDING_VERIFICATION` until admin reviews and activates

---

## Phase 1b: Admin Review & Activation (Platform Admin)

**Note**: This phase is for platform admins only. After vendors submit applications, admins review and activate them.

### Step 1: Get Pending Vendors

**Endpoint**: `GET /vendors/admin/pending-vendors`

**Authorization**: `ADMIN` or `SUPER_ADMIN` role required

**Response** (200 OK):
```json
[
  {
    "id": 123,
    "email": "vendor@example.com",
    "phone": "+1234567890",
    "address": "123 Main St",
    "zipCode": "12345",
    "businessRegistrationId": "REG123456",
    "onboardingStatus": "PENDING_VERIFICATION",
    "status": "INACTIVE"
  }
]
```

---

## Phase 1b: Admin Review & Activation (Platform Admin)

**Note**: After vendors submit applications via `/vendors/apply`, platform admins review them, contact the responsible person, and then activate the vendor account.

### Verify and Activate Vendor

**Endpoint**: `PUT /vendors/admin/{vendorId}/verify-and-activate`

**Note**: After reviewing the vendor application and contacting the responsible person, admins activate the vendor account and credentials are sent via email.

**Authorization**: `ADMIN` or `SUPER_ADMIN` role required

**Path Parameters**:
- `vendorId`: ID of the vendor to verify

**Response** (200 OK):
```json
{
  "email": "vendor@example.com",
  "temporaryPassword": "SecurePass123!",
  "loginUrl": "http://localhost:8083/auth/login",
  "message": "Vendor verified and activated. Credentials sent to email."
}
```

**Important**: 
- Credentials are also sent via email to the vendor
- Vendor should change password on first login
- Vendor status changes to `ACTIVE` and onboarding status to `VERIFIED`

**Error Responses**:
- `400 Bad Request`: Vendor not in PENDING_VERIFICATION status
- `404 Not Found`: Vendor ID not found

---

## Phase 2: Vendor Onboarding Steps

After receiving credentials via email, the vendor logs in and completes onboarding.

### Step 1: Get Onboarding Status

**Endpoint**: `GET /vendors/onboarding/status`

**Authorization**: `VENDOR_ADMIN` role required

**Response** (200 OK):
```json
{
  "status": "VERIFIED",
  "isChainLocation": false,
  "isUniqueVendor": true,
  "chainName": null,
  "isHeadquarters": false,
  "usesSharedPaymentAccount": false
}
```

### Step 2: Set Vendor Type

**Endpoint**: `POST /vendors/onboarding/set-vendor-type`

**Authorization**: `VENDOR_ADMIN` role required

**Request Body** (CHAIN):
```json
{
  "vendorType": "CHAIN",
  "chainName": "McDonald's"
}
```

**Request Body** (UNIQUE):
```json
{
  "vendorType": "UNIQUE",
  "chainName": null
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Vendor type set successfully"
}
```

**Status Transition**: `VERIFIED` → `TYPE_SELECTED`

### Step 3: Add Headquarters (CHAIN only)

**Endpoint**: `POST /vendors/onboarding/add-headquarters`

**Authorization**: `VENDOR_ADMIN` role required (CHAIN vendors only)

**Request Body**:
```json
{
  "locationName": "Downtown Location",
  "address": "456 Main St",
  "zipCode": "12345",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "phone": "+1234567890",  // Optional
  "country": "Croatia"  // Required: Country name or ISO code (e.g., "Croatia", "Hrvatska", "HR", "United States", "USA", "US", "Germany", "Deutschland", "DE") - automatically converted to ISO 2-letter code
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Headquarters added successfully"
}
```

**Status Transition**: `TYPE_SELECTED` → `HEADQUARTERS_ADDED`

**Note**: For UNIQUE vendors, skip this step and proceed to Step 4.

### Step 4: Set Banking Model

**Endpoint**: `POST /vendors/onboarding/set-banking-model`

**Authorization**: `VENDOR_ADMIN` role required

**Request Body** (SHARED - CHAIN only):
```json
{
  "bankingModel": "SHARED"
}
```

**Request Body** (INDIVIDUAL - CHAIN):
```json
{
  "bankingModel": "INDIVIDUAL"
}
```

**Request Body** (INDIVIDUAL - UNIQUE):
```json
{
  "bankingModel": "INDIVIDUAL",
  "country": "Croatia"  // Required for UNIQUE vendors: Country name or ISO code (e.g., "Croatia", "Hrvatska", "HR", "United States", "USA", "US", "Germany", "Deutschland", "DE") - automatically converted to ISO 2-letter code
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Banking model set successfully"
}
```

**Status Transition**: 
- CHAIN: `HEADQUARTERS_ADDED` → `BANKING_SETUP`
- UNIQUE: `TYPE_SELECTED` → `BANKING_SETUP`

**Important**:
- UNIQUE vendors cannot use SHARED model (will return error)
- SHARED model requires headquarters to have payment account setup first
- **UNIQUE vendors must provide `country` field** in the request body (CHAIN vendors have country set during headquarters step)
- Country is required for payment account setup (determines payment provider: Stripe vs MoR)
- **Country format**: Accepts country names in multiple languages (e.g., "Croatia", "Hrvatska"), ISO 2-letter codes (e.g., "HR"), or ISO 3-letter codes (e.g., "HRV") - automatically converted to ISO 2-letter code internally

### Step 5: Setup Payment Account

**Endpoint**: `POST /vendors/onboarding/setup-payment-account`

**Authorization**: `VENDOR_ADMIN` role required

**Request Body**: None (uses vendor's country from profile)

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Payment account setup initiated successfully"
}
```

**Status Transition**: `BANKING_SETUP` → `PAYMENT_CONFIGURED`

**Note**: 
- Payment account setup is asynchronous
- For SHARED model, only headquarters should setup payment account
- Country must be set in vendor profile before this step

### Step 6: Complete Onboarding

**Endpoint**: `POST /vendors/onboarding/complete`

**Authorization**: `VENDOR_ADMIN` role required

**Request Body**: None

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Onboarding completed successfully! You can now fully use the platform."
}
```

**Status Transition**: `PAYMENT_CONFIGURED` → `COMPLETED`

---

## Phase 3: Chain Location Management

**Note**: Only available for CHAIN vendors after onboarding is completed.

### Add New Location

**Endpoint**: `POST /vendors/chain/locations`

**Authorization**: `VENDOR_ADMIN` role required

**Request Body**:
```json
{
  "locationName": "Airport Location",
  "email": "airport@mcdonalds.com",
  "phone": "+1234567891",
  "address": "789 Airport Blvd",
  "zipCode": "12346",
  "latitude": 40.7500,
  "longitude": -74.0000,
  "country": "United States"  // Optional, uses chain's default if not provided. Accepts country names or ISO codes (e.g., "United States", "USA", "US", "Croatia", "Hrvatska", "HR")
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Location added successfully. Credentials sent to: airport@mcdonalds.com"
}
```

**Important**:
- Creates a new VENDOR_ADMIN account for the location
- Credentials are sent via email
- Payment account is automatically setup based on banking model:
  - **SHARED**: Links to headquarters payment account
  - **INDIVIDUAL**: Sets up separate payment account

### Get All Chain Locations

**Endpoint**: `GET /vendors/chain/locations`

**Authorization**: `VENDOR_ADMIN` role required

**Response** (200 OK):
```json
[
  {
    "id": 124,
    "chainId": "uuid-chain-id",
    "chainName": "McDonald's",
    "locationName": "Downtown Location",
    "email": "downtown@mcdonalds.com",
    "isHeadquarters": true,
    "usesSharedPaymentAccount": true,
    "status": "ACTIVE"
  },
  {
    "id": 125,
    "chainId": "uuid-chain-id",
    "chainName": "McDonald's",
    "locationName": "Airport Location",
    "email": "airport@mcdonalds.com",
    "isHeadquarters": false,
    "usesSharedPaymentAccount": true,
    "status": "ACTIVE"
  }
]
```

### Update Location

**Endpoint**: `PUT /vendors/chain/locations/{locationId}`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `locationId`: ID of the location to update

**Request Body**: Same as Add Location

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Location updated successfully"
}
```

**Note**: Cannot update headquarters using this endpoint.

### Remove Location

**Endpoint**: `DELETE /vendors/chain/locations/{locationId}`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `locationId`: ID of the location to remove

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Location removed successfully"
}
```

**Important**:
- Location is deactivated (not deleted)
- All offers for this location are invalidated
- Cannot remove headquarters
- Cannot remove own location

---

## Phase 4: Banking Model Management

### Get Banking Model Information

**Endpoint**: `GET /vendors/chain/banking/info`

**Authorization**: `VENDOR_ADMIN` role required

**Response** (200 OK):
```json
{
  "bankingModel": "SHARED",
  "chainName": "McDonald's",
  "totalLocations": 3,
  "locationsWithPaymentAccounts": 3,
  "headquartersHasAccount": true
}
```

### Switch Banking Model

**Endpoint**: `PUT /vendors/chain/banking/switch-model`

**Authorization**: `VENDOR_ADMIN` role required

**Request Body** (Switch to SHARED):
```json
{
  "bankingModel": "SHARED"
}
```

**Request Body** (Switch to INDIVIDUAL):
```json
{
  "bankingModel": "INDIVIDUAL"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Banking model switched to SHARED successfully"
}
```

**Important**:
- **Switching to SHARED**: Requires headquarters to have payment account
- **Switching to INDIVIDUAL**: Requires ALL locations to have payment accounts
- Returns error if requirements not met

### Setup Location Payment Account

**Endpoint**: `POST /vendors/chain/locations/{locationId}/setup-payment-account`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `locationId`: ID of the location

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Payment account setup initiated for location"
}
```

**Note**: Only used when banking model is INDIVIDUAL.

---

## Phase 5: Worker Management

Workers (VENDOR role) can only manage offers for their assigned location.

**⚠️ Important Security Restriction**: 
- **Headquarters VENDOR_ADMIN**: Can manage workers for all locations in the chain
- **Non-Headquarters VENDOR_ADMIN**: Can only manage workers for their own location

### Create Worker

**Endpoint**: `POST /vendors/chain/locations/{locationId}/workers`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `locationId`: ID of the location to assign worker to

**Request Body**:
```json
{
  "username": "worker_john",
  "email": "worker@example.com",
  "password": "SecurePass123!",
  "phone": "+1234567892"  // Optional
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Worker created successfully. Credentials sent to: worker@example.com"
}
```

**Error Responses**:
- `400 Bad Request`: "Only headquarters can create workers for other locations. You can only create workers for your own location." (when non-headquarters tries to create worker for different location)

**Important**:
- Worker is assigned to the specified location
- Worker can only create/manage offers for that location
- Credentials are sent via email
- **Headquarters can create workers for any location in the chain**
- **Non-headquarters can only create workers for their own location** (`locationId` must equal current user's vendor ID)

### Get Location Workers

**Endpoint**: `GET /vendors/chain/locations/{locationId}/workers`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `locationId`: ID of the location

**Response** (200 OK):
```json
[
  {
    "id": 126,
    "username": "worker_john",
    "email": "worker@example.com",
    "role": "VENDOR",
    "assignedLocationId": 124,
    "status": "ACTIVE"
  }
]
```

**Error Responses**:
- `400 Bad Request`: "Only headquarters can access workers for other locations. You can only access workers for your own location." (when non-headquarters tries to access workers for different location)

**Important**:
- **Headquarters can access workers for any location in the chain**
- **Non-headquarters can only access workers for their own location** (`locationId` must equal current user's vendor ID)

### Activate Worker

**Endpoint**: `PUT /vendors/chain/workers/{workerId}/activate`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `workerId`: ID of the worker

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Worker activated successfully"
}
```

**Error Responses**:
- `400 Bad Request`: "Only headquarters can activate workers for other locations. You can only activate workers for your own location." (when non-headquarters tries to activate worker from different location)

**Important**:
- **Headquarters can activate workers for any location in the chain**
- **Non-headquarters can only activate workers for their own location** (worker's `assignedLocationId` must equal current user's vendor ID)

### Deactivate Worker

**Endpoint**: `PUT /vendors/chain/workers/{workerId}/deactivate`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `workerId`: ID of the worker

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Worker deactivated successfully"
}
```

**Error Responses**:
- `400 Bad Request`: "Only headquarters can deactivate workers for other locations. You can only deactivate workers for your own location." (when non-headquarters tries to deactivate worker from different location)

**Important**:
- **Headquarters can deactivate workers for any location in the chain**
- **Non-headquarters can only deactivate workers for their own location** (worker's `assignedLocationId` must equal current user's vendor ID)

### Delete Worker

**Endpoint**: `DELETE /vendors/chain/workers/{workerId}`

**Authorization**: `VENDOR_ADMIN` role required

**Path Parameters**:
- `workerId`: ID of the worker

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Worker deleted successfully"
}
```

**Error Responses**:
- `400 Bad Request`: "Only headquarters can delete workers for other locations. You can only delete workers for your own location." (when non-headquarters tries to delete worker from different location)

**Important**: 
- All offers created by the worker are invalidated upon deletion
- **Headquarters can delete workers for any location in the chain**
- **Non-headquarters can only delete workers for their own location** (worker's `assignedLocationId` must equal current user's vendor ID)

### Worker Management Permissions

**How to Check if User is Headquarters:**

The login response includes vendor information with `isHeadquarters` field:

```json
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "VENDOR_ADMIN",
  "vendor": {
    "id": 20,
    "email": "hq@example.com",
    "isHeadquarters": true,  // ← Check this field
    "isChainLocation": true,
    "chainName": "McDonald's",
    "locationName": "Downtown Location"
  }
}
```

**Kotlin Implementation Example:**

```kotlin
data class VendorInfo(
    val id: Long,
    val email: String,
    val isHeadquarters: Boolean?,
    val isChainLocation: Boolean?,
    val chainName: String?,
    val locationName: String?
)

// Check if user can manage workers for a specific location
fun canManageWorkersForLocation(
    currentVendor: VendorInfo?, 
    targetLocationId: Long
): Boolean {
    if (currentVendor == null) return false
    
    // If not part of a chain, can only manage own location
    if (currentVendor.isChainLocation != true) {
        return currentVendor.id == targetLocationId
    }
    
    // If headquarters, can manage workers for any location in chain
    if (currentVendor.isHeadquarters == true) {
        return true  // Headquarters can manage all locations
    }
    
    // Non-headquarters can only manage own location
    return currentVendor.id == targetLocationId
}

// Usage in UI
val canCreateWorker = canManageWorkersForLocation(
    currentUser.vendorInfo, 
    selectedLocationId
)

if (!canCreateWorker) {
    // Show error: "Only headquarters can create workers for other locations"
    // Or disable the create worker button
}
```

**UI Recommendations:**

1. **Location Selection**:
   - **Headquarters**: Show dropdown/list of all chain locations
   - **Non-headquarters**: Auto-select own location, hide location selector

2. **Worker List**:
   - **Headquarters**: Show workers grouped by location, allow filtering by location
   - **Non-headquarters**: Only show workers for own location

3. **Error Handling**:
   - Show clear error message: "Only headquarters can manage workers for other locations"
   - Provide contact information for headquarters if needed

---

## Upgrade Unique Vendor to Chain

**Endpoint**: `POST /vendors/upgrade-to-chain?chainName={chainName}`

**Authorization**: `VENDOR_ADMIN` role required

**Query Parameters**:
- `chainName`: Name of the chain (e.g., "My Restaurant Chain")

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Vendor upgraded to chain successfully. You can now add multiple locations."
}
```

**Important**:
- Only available for UNIQUE vendors
- Current location becomes headquarters
- Enables all chain management features

---

## API Endpoints Reference

### Base URL
```
http://localhost:8083/vendors
```

### Authentication
All endpoints (except public registration) require JWT token:
```
Authorization: Bearer <jwt_token>
```

### Common Response Formats

**Success Response**:
```json
{
  "success": true,
  "message": "Operation completed successfully"
}
```

**Error Response**:
```json
{
  "message": "Error description"
}
```

### HTTP Status Codes

- `200 OK`: Success
- `400 Bad Request`: Validation error or invalid state
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists
- `500 Internal Server Error`: Server error

---

## Error Handling

### Common Error Scenarios

1. **Invalid Onboarding Status**
   ```json
   {
     "message": "Vendor type must be selected before adding headquarters. Current status: VERIFIED"
   }
   ```
   **Solution**: Check onboarding status and guide user to correct step.

2. **Chain Validation Error**
   ```json
   {
     "message": "Only CHAIN vendors can add headquarters. Current vendor type: UNIQUE"
   }
   ```
   **Solution**: Show appropriate UI based on vendor type.

3. **Banking Model Validation**
   ```json
   {
     "message": "Headquarters must have a payment account before switching to SHARED model."
   }
   ```
   **Solution**: Guide user to setup payment account first.

4. **Security Violation**
   ```json
   {
     "message": "Cannot access workers for location from different chain. Security violation."
   }
   ```
   **Solution**: This should not happen in normal flow, but handle gracefully.

### Error Handling Best Practices

1. **Always check onboarding status** before allowing actions
2. **Validate vendor type** (CHAIN vs UNIQUE) before showing options
3. **Handle async operations** (payment setup) with loading states
4. **Show clear error messages** to guide users
5. **Implement retry logic** for network errors
6. **Cache onboarding status** to reduce API calls

---

## UI/UX Recommendations

### Onboarding Flow UI

1. **Progress Indicator**
   - Show current step (e.g., "Step 2 of 6")
   - Display onboarding status
   - Allow navigation back to previous steps (if valid)

2. **Vendor Type Selection**
   - Clear explanation of CHAIN vs UNIQUE
   - Show benefits of each option
   - Allow change before completion

3. **Headquarters Setup**
   - Map integration for location selection
   - Address autocomplete
   - Coordinate validation

4. **Banking Model Selection**
   - Clear explanation of SHARED vs INDIVIDUAL
   - Show requirements for each model
   - Visual comparison

5. **Payment Account Setup**
   - Show loading state during setup
   - Display setup progress
   - Handle errors gracefully

### Chain Management UI

1. **Location List**
   - Show all locations in chain
   - Highlight headquarters
   - Display location status
   - Quick actions (edit, remove)

2. **Add Location**
   - Form with validation
   - Map integration
   - Preview before submission

3. **Worker Management**
   - List workers per location
   - Quick actions (activate/deactivate/delete)
   - Show worker activity
   - **Permission-based UI**: 
     - Headquarters: Show all locations with workers, allow managing workers for any location
     - Non-headquarters: Only show own location's workers, restrict location selection to own location

### State Management

```kotlin
data class VendorOnboardingState(
    val status: OnboardingStatus,
    val isChainLocation: Boolean,
    val isUniqueVendor: Boolean,
    val chainName: String?,
    val isHeadquarters: Boolean,
    val usesSharedPaymentAccount: Boolean?
)

// Example state machine
fun getNextStep(state: VendorOnboardingState): OnboardingStep? {
    return when (state.status) {
        OnboardingStatus.VERIFIED -> OnboardingStep.SELECT_VENDOR_TYPE
        OnboardingStatus.TYPE_SELECTED -> {
            if (state.isChainLocation) OnboardingStep.ADD_HEADQUARTERS
            else OnboardingStep.SET_BANKING_MODEL
        }
        OnboardingStatus.HEADQUARTERS_ADDED -> OnboardingStep.SET_BANKING_MODEL
        OnboardingStatus.BANKING_SETUP -> OnboardingStep.SETUP_PAYMENT_ACCOUNT
        OnboardingStatus.PAYMENT_CONFIGURED -> OnboardingStep.COMPLETE_ONBOARDING
        OnboardingStatus.COMPLETED -> null
        else -> null
    }
}
```

### Navigation Flow

```
Login Screen
    ↓
Onboarding Status Check
    ↓
┌─────────────────────────────────────┐
│  If COMPLETED: Main Dashboard      │
│  Else: Onboarding Flow              │
└─────────────────────────────────────┘
    ↓
Onboarding Steps (guided flow)
    ↓
Completion Screen
    ↓
Main Dashboard
```

---

## Testing Checklist

### Admin Flow
- [ ] Register pending vendor
- [ ] List pending vendors
- [ ] Verify and activate vendor
- [ ] Verify email credentials sent

### Vendor Onboarding Flow
- [ ] Login with credentials
- [ ] Check onboarding status
- [ ] Select vendor type (CHAIN)
- [ ] Add headquarters
- [ ] Set banking model
- [ ] Setup payment account
- [ ] Complete onboarding

### Chain Management
- [ ] Add new location
- [ ] List all locations
- [ ] Update location
- [ ] Remove location
- [ ] Switch banking model
- [ ] Setup individual payment accounts

### Worker Management
- [ ] Create worker
- [ ] List workers
- [ ] Activate/deactivate worker
- [ ] Delete worker
- [ ] Verify worker can only see assigned location offers

### Edge Cases
- [ ] Upgrade unique to chain
- [ ] Error handling for invalid states
- [ ] Security validation (cross-chain access)
- [ ] Network error handling
- [ ] Offline state handling

---

## Additional Notes

1. **Payment Account Setup**: This is asynchronous. Poll status or use webhooks if available.

2. **Email Notifications**: All credentials are sent via email. Consider showing a "Check your email" message.

3. **Password Change**: Vendors should change password on first login.

4. **Location Coordinates**: Use map picker or geocoding service for accurate coordinates.

5. **Banking Model**: Once set, changing requires all locations to have accounts (for INDIVIDUAL).

6. **Worker Permissions**: Workers can only manage offers for their assigned location. The backend automatically routes offer operations to the correct location.

7. **Caching**: Cache onboarding status and vendor information to reduce API calls.

---

## Support & Troubleshooting

### Common Issues

**Issue**: "Vendor type must be selected before adding headquarters"
- **Cause**: Onboarding status is not TYPE_SELECTED
- **Solution**: Complete vendor type selection first

**Issue**: "Only CHAIN vendors can add headquarters"
- **Cause**: Vendor is UNIQUE type
- **Solution**: Upgrade to chain first, or skip headquarters step

**Issue**: "Headquarters must have payment account before switching to SHARED"
- **Cause**: Payment account not setup
- **Solution**: Complete payment account setup for headquarters first

**Issue**: "All locations must have payment accounts before switching to INDIVIDUAL"
- **Cause**: Some locations missing payment accounts
- **Solution**: Setup payment accounts for all locations first

---

## API Integration Example (Kotlin)

```kotlin
// Example: Complete onboarding flow
class VendorOnboardingRepository {
    suspend fun getOnboardingStatus(): OnboardingStatusResponse {
        return apiService.getOnboardingStatus()
    }
    
    suspend fun setVendorType(type: VendorType, chainName: String?): ApiResponse {
        return apiService.setVendorType(VendorTypeRequest(type, chainName))
    }
    
    suspend fun addHeadquarters(request: HeadquartersRequest): ApiResponse {
        return apiService.addHeadquarters(request)
    }
    
    suspend fun setBankingModel(model: BankingModel): ApiResponse {
        return apiService.setBankingModel(BankingModelRequest(model))
    }
    
    suspend fun setupPaymentAccount(): ApiResponse {
        return apiService.setupPaymentAccount()
    }
    
    suspend fun completeOnboarding(): ApiResponse {
        return apiService.completeOnboarding()
    }
}

// Example: Chain location management
suspend fun addChainLocation(request: LocationRequest): ApiResponse {
    return apiService.addChainLocation(request)
}

suspend fun getChainLocations(): List<Location> {
    return apiService.getChainLocations()
}

// Example: Worker management
suspend fun createWorker(locationId: Long, request: WorkerRequest): ApiResponse {
    return apiService.createWorker(locationId, request)
}
```

---

**Last Updated**: [Current Date]
**Version**: 1.0
**API Base URL**: `http://localhost:8083/vendors`

