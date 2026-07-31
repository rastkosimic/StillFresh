# Android Banking Model Switching Implementation Prompt

## Overview

This document describes how to implement banking model switching functionality in the Android vendor application. The banking model determines whether chain locations use a **SHARED** payment account (headquarters account) or **INDIVIDUAL** payment accounts (each location has its own account).

**Critical Security Restriction**: Only **Headquarters VENDOR_ADMIN** can switch banking models. Non-headquarters locations cannot perform this action and must contact headquarters to request changes.

## Table of Contents

1. [Business Logic Overview](#business-logic-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Checking User Permissions](#checking-user-permissions)
4. [API Endpoint Reference](#api-endpoint-reference)
5. [Implementation Steps](#implementation-steps)
6. [Error Handling](#error-handling)
7. [UI/UX Recommendations](#uiux-recommendations)
8. [State Management](#state-management)
9. [Testing Checklist](#testing-checklist)

---

## Business Logic Overview

### Banking Models

1. **SHARED Model**
   - All chain locations use the headquarters payment account
   - Payments from all locations are routed to headquarters Stripe account
   - Locations don't need to manage individual payment accounts
   - Headquarters manages all payment processing

2. **INDIVIDUAL Model**
   - Each location has its own payment account
   - Payments from each location are routed to that location's Stripe account
   - Each location must set up and manage their own payment account
   - Offers are automatically invalidated when switching to INDIVIDUAL (locations must reactivate after setting up accounts)

### Who Can Switch?

- ✅ **Headquarters VENDOR_ADMIN**: Can switch between SHARED and INDIVIDUAL
- ❌ **Non-Headquarters VENDOR_ADMIN**: Cannot switch (will receive error message)
- ❌ **VENDOR (workers)**: Cannot switch (not authorized)

### Impact of Switching

**When switching from SHARED → INDIVIDUAL:**
- All locations are updated to use individual accounts
- **All active offers from all chain locations are automatically invalidated**
- Location managers receive email and in-app notifications
- Locations must set up individual payment accounts before reactivating offers

**When switching from INDIVIDUAL → SHARED:**
- All locations are updated to use headquarters shared account
- Location managers receive email and in-app notifications
- No offer invalidation (locations can continue using offers)

---

## Authentication & Authorization

### Base Configuration

- **Base URL**: `http://localhost:8083` (development) or your production API gateway URL
- **Authentication**: JWT Bearer token required
- **Content-Type**: `application/json` for request bodies
- **Response Format**: JSON

### Required Role

- **Minimum Role**: `VENDOR_ADMIN`
- **Additional Requirement**: Must be Headquarters (checked server-side)

---

## Checking User Permissions

### Step 1: Get Current User Info from Login Response

When the user logs in, the authentication response includes vendor information:

**Endpoint**: `POST /auth/login` (via authorization-service)

**Response** (200 OK):
```json
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "VENDOR_ADMIN",
  "vendor": {
    "id": 20,
    "email": "hq@example.com",
    "isHeadquarters": true,
    "isChainLocation": true,
    "isUniqueVendor": false,
    "chainName": "McDonald's",
    "locationName": "Downtown Location",
    "usesSharedPaymentAccount": true
  }
}
```

**Key Fields for Banking Model Switching:**
- `vendor.isHeadquarters` (boolean): `true` if user is headquarters, `false` otherwise
- `vendor.isChainLocation` (boolean): `true` if part of a chain
- `vendor.usesSharedPaymentAccount` (boolean): Current banking model (`true` = SHARED, `false` = INDIVIDUAL)

### Step 2: Check Onboarding Status (Alternative)

If you need to refresh the vendor info, you can also check onboarding status:

**Endpoint**: `GET /vendors/onboarding/status`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Response** (200 OK):
```json
{
  "status": "COMPLETED",
  "isChainLocation": true,
  "isUniqueVendor": false,
  "chainName": "McDonald's",
  "isHeadquarters": true,
  "usesSharedPaymentAccount": true
}
```

### Step 3: Determine UI Visibility

```kotlin
// Example Kotlin logic
data class VendorInfo(
    val id: Long,
    val email: String,
    val isHeadquarters: Boolean,
    val isChainLocation: Boolean,
    val chainName: String?,
    val locationName: String?,
    val usesSharedPaymentAccount: Boolean?
)

fun canSwitchBankingModel(vendorInfo: VendorInfo?): Boolean {
    return vendorInfo?.isHeadquarters == true && 
           vendorInfo.isChainLocation == true
}

// Usage in UI
if (canSwitchBankingModel(currentUser.vendorInfo)) {
    // Show banking model switching UI
} else {
    // Hide or disable banking model switching UI
    // Show message: "Only headquarters can switch banking models"
}
```

---

## API Endpoint Reference

### Get Current Banking Model

**Endpoint**: `GET /vendors/chain/banking/info`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Authorization**: `VENDOR_ADMIN` role required

**Response** (200 OK):
```json
{
  "bankingModel": "SHARED",
  "chainName": "McDonald's",
  "totalLocations": 5,
  "headquartersEmail": "hq@example.com",
  "headquartersLocationName": "Downtown Location"
}
```

**Response Fields**:
- `bankingModel` (string): Current model - `"SHARED"` or `"INDIVIDUAL"`
- `chainName` (string): Name of the chain
- `totalLocations` (number): Total number of locations in the chain
- `headquartersEmail` (string): Email of headquarters location
- `headquartersLocationName` (string): Name of headquarters location

**Error Responses**:
- `400 Bad Request`: Vendor is not part of a chain
- `401 Unauthorized`: Invalid or missing JWT token
- `403 Forbidden`: User is not VENDOR_ADMIN

### Switch Banking Model

**Endpoint**: `PUT /vendors/chain/banking/switch-model`

**Headers**:
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Authorization**: 
- `VENDOR_ADMIN` role required
- **Must be Headquarters** (enforced server-side)

**Request Body**:
```json
{
  "bankingModel": "INDIVIDUAL"
}
```

**Valid Values for `bankingModel`**:
- `"SHARED"`: All locations use headquarters payment account
- `"INDIVIDUAL"`: Each location uses its own payment account

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Banking model switched to INDIVIDUAL successfully"
}
```

**Error Responses**:

1. **403 Forbidden - Not Headquarters**:
```json
{
  "error": "Only Headquarters VENDOR_ADMIN can switch banking model. Please contact your chain headquarters administrator to request this change."
}
```
**HTTP Status**: `400 Bad Request`

2. **400 Bad Request - Not VENDOR_ADMIN**:
```json
{
  "error": "Only VENDOR_ADMIN can switch banking model"
}
```

3. **400 Bad Request - Not Part of Chain**:
```json
{
  "error": "Only chain vendors can switch banking models. Unique vendors use INDIVIDUAL model."
}
```

4. **400 Bad Request - No Change**:
```json
{
  "error": "Banking model is already set to INDIVIDUAL. No change needed."
}
```

5. **500 Internal Server Error**:
```json
{
  "error": "Failed to switch banking model: <error message>"
}
```

---

## Implementation Steps

### Step 1: Create Data Models

```kotlin
// BankingModel.kt
enum class BankingModel(val value: String) {
    SHARED("SHARED"),
    INDIVIDUAL("INDIVIDUAL")
}

// BankingModelInfo.kt
data class BankingModelInfo(
    val bankingModel: String,
    val chainName: String,
    val totalLocations: Int,
    val headquartersEmail: String,
    val headquartersLocationName: String
)

// SwitchBankingModelRequest.kt
data class SwitchBankingModelRequest(
    val bankingModel: String
)

// ApiResponse.kt
data class ApiResponse(
    val success: Boolean,
    val message: String
)

// ErrorResponse.kt
data class ErrorResponse(
    val error: String
)
```

### Step 2: Create API Service Interface

```kotlin
// VendorBankingApiService.kt
interface VendorBankingApiService {
    
    @GET("vendors/chain/banking/info")
    suspend fun getBankingModelInfo(
        @Header("Authorization") token: String
    ): Response<BankingModelInfo>
    
    @PUT("vendors/chain/banking/switch-model")
    suspend fun switchBankingModel(
        @Header("Authorization") token: String,
        @Body request: SwitchBankingModelRequest
    ): Response<ApiResponse>
}
```

### Step 3: Create Repository

```kotlin
// VendorBankingRepository.kt
class VendorBankingRepository(
    private val apiService: VendorBankingApiService,
    private val tokenManager: TokenManager
) {
    
    suspend fun getBankingModelInfo(): Result<BankingModelInfo> {
        return try {
            val token = tokenManager.getToken() ?: return Result.failure(
                Exception("No authentication token")
            )
            val response = apiService.getBankingModelInfo("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Failed to get banking model info: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun switchBankingModel(newModel: BankingModel): Result<String> {
        return try {
            val token = tokenManager.getToken() ?: return Result.failure(
                Exception("No authentication token")
            )
            val request = SwitchBankingModelRequest(newModel.value)
            val response = apiService.switchBankingModel("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.message)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorResponse(errorBody)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseErrorResponse(errorBody: String?): String {
        return try {
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            errorResponse.error ?: "Unknown error occurred"
        } catch (e: Exception) {
            errorBody ?: "Unknown error occurred"
        }
    }
}
```

### Step 4: Create ViewModel

```kotlin
// BankingModelViewModel.kt
class BankingModelViewModel(
    private val repository: VendorBankingRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _bankingModelInfo = MutableStateFlow<BankingModelInfo?>(null)
    val bankingModelInfo: StateFlow<BankingModelInfo?> = _bankingModelInfo
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage
    
    val canSwitchBankingModel: Boolean
        get() = userRepository.getCurrentUser()?.vendorInfo?.isHeadquarters == true &&
                userRepository.getCurrentUser()?.vendorInfo?.isChainLocation == true
    
    init {
        loadBankingModelInfo()
    }
    
    fun loadBankingModelInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.getBankingModelInfo()
                .onSuccess { info ->
                    _bankingModelInfo.value = info
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load banking model info"
                }
            
            _isLoading.value = false
        }
    }
    
    fun switchBankingModel(newModel: BankingModel) {
        if (!canSwitchBankingModel) {
            _errorMessage.value = "Only headquarters can switch banking models"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            repository.switchBankingModel(newModel)
                .onSuccess { message ->
                    _successMessage.value = message
                    loadBankingModelInfo() // Refresh info after switch
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to switch banking model"
                }
            
            _isLoading.value = false
        }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
```

### Step 5: Create UI Screen

```kotlin
// BankingModelScreen.kt
@Composable
fun BankingModelScreen(
    viewModel: BankingModelViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val bankingModelInfo by viewModel.bankingModelInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val canSwitch = viewModel.canSwitchBankingModel
    
    LaunchedEffect(Unit) {
        viewModel.loadBankingModelInfo()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Banking Model") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                bankingModelInfo != null -> {
                    BankingModelContent(
                        info = bankingModelInfo!!,
                        canSwitch = canSwitch,
                        onSwitchModel = { newModel ->
                            viewModel.switchBankingModel(newModel)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text(
                        "Failed to load banking model information",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            // Error message
            errorMessage?.let { error ->
                LaunchedEffect(error) {
                    // Show snackbar or dialog
                }
            }
            
            // Success message
            successMessage?.let { success ->
                LaunchedEffect(success) {
                    // Show snackbar
                }
            }
        }
    }
}

@Composable
fun BankingModelContent(
    info: BankingModelInfo,
    canSwitch: Boolean,
    onSwitchModel: (BankingModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentModel = if (info.bankingModel == "SHARED") {
        BankingModel.SHARED
    } else {
        BankingModel.INDIVIDUAL
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current model card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Current Banking Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    currentModel.value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Chain: ${info.chainName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Total Locations: ${info.totalLocations}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Model descriptions
        BankingModelDescriptionCard(
            model = BankingModel.SHARED,
            isSelected = currentModel == BankingModel.SHARED
        )
        
        BankingModelDescriptionCard(
            model = BankingModel.INDIVIDUAL,
            isSelected = currentModel == BankingModel.INDIVIDUAL
        )
        
        // Warning for INDIVIDUAL model
        if (currentModel == BankingModel.INDIVIDUAL) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "⚠️ Important",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "When switching to INDIVIDUAL model, all active offers from all chain locations will be automatically invalidated. Locations must set up their individual payment accounts and reactivate offers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // Switch button (only for headquarters)
        if (canSwitch) {
            val targetModel = if (currentModel == BankingModel.SHARED) {
                BankingModel.INDIVIDUAL
            } else {
                BankingModel.SHARED
            }
            
            Button(
                onClick = {
                    // Show confirmation dialog
                    onSwitchModel(targetModel)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Switch to ${targetModel.value} Model")
            }
        } else {
            // Info card for non-headquarters
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "ℹ️ Banking Model Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Only headquarters can switch banking models. If you need to change the banking model, please contact your chain headquarters administrator.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Headquarters: ${info.headquartersLocationName} (${info.headquartersEmail})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BankingModelDescriptionCard(
    model: BankingModel,
    isSelected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                model.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            when (model) {
                BankingModel.SHARED -> {
                    Text("• All locations use headquarters payment account")
                    Text("• Payments routed to headquarters Stripe account")
                    Text("• Locations don't need individual payment accounts")
                }
                BankingModel.INDIVIDUAL -> {
                    Text("• Each location has its own payment account")
                    Text("• Payments routed to each location's Stripe account")
                    Text("• Each location must manage their own account")
                }
            }
        }
    }
}
```

---

## Error Handling

### Common Error Scenarios

1. **User is not Headquarters**
   - **Error**: "Only Headquarters VENDOR_ADMIN can switch banking model..."
   - **UI Action**: Show info message explaining restriction, provide headquarters contact info

2. **User is not VENDOR_ADMIN**
   - **Error**: "Only VENDOR_ADMIN can switch banking model"
   - **UI Action**: Hide banking model switching UI entirely

3. **User is not part of a chain**
   - **Error**: "Only chain vendors can switch banking models..."
   - **UI Action**: Hide banking model switching UI (unique vendors use INDIVIDUAL by default)

4. **Network errors**
   - **UI Action**: Show retry button, allow user to try again

5. **Server errors**
   - **UI Action**: Show generic error message, log error for debugging

### Error Handling Example

```kotlin
fun handleBankingModelError(error: Throwable): String {
    return when {
        error.message?.contains("Only Headquarters") == true -> {
            "Only headquarters can switch banking models. Please contact your chain headquarters administrator."
        }
        error.message?.contains("Only VENDOR_ADMIN") == true -> {
            "You don't have permission to switch banking models."
        }
        error.message?.contains("chain vendors") == true -> {
            "Banking model switching is only available for chain vendors."
        }
        error.message?.contains("already set") == true -> {
            "The banking model is already set to the selected model."
        }
        else -> {
            "An error occurred while switching banking model. Please try again later."
        }
    }
}
```

---

## UI/UX Recommendations

### 1. Permission-Based UI Visibility

- **Headquarters VENDOR_ADMIN**: Show full banking model management UI with switch button
- **Non-Headquarters VENDOR_ADMIN**: Show read-only view with info message and headquarters contact
- **VENDOR (workers)**: Hide banking model section entirely

### 2. Confirmation Dialog

Always show a confirmation dialog before switching, especially when switching to INDIVIDUAL:

```kotlin
@Composable
fun BankingModelSwitchConfirmationDialog(
    currentModel: BankingModel,
    targetModel: BankingModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Switch Banking Model?")
        },
        text = {
            Column {
                Text("You are about to switch from ${currentModel.value} to ${targetModel.value}.")
                Spacer(modifier = Modifier.height(8.dp))
                if (targetModel == BankingModel.INDIVIDUAL) {
                    Text(
                        "⚠️ WARNING: All active offers from all chain locations will be automatically invalidated. Locations must set up individual payment accounts and reactivate offers.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Switch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### 3. Loading States

- Show loading indicator during API calls
- Disable switch button while loading
- Show progress if operation takes time

### 4. Success Feedback

- Show success snackbar after successful switch
- Refresh banking model info automatically
- Show updated model immediately

### 5. Visual Indicators

- Highlight current model with different color/border
- Use icons to distinguish SHARED vs INDIVIDUAL
- Show warning badges for important information

### 6. Navigation

- Add banking model section to chain management screen
- Allow navigation from chain locations list
- Provide back navigation to previous screen

---

## State Management

### Recommended State Structure

```kotlin
data class BankingModelState(
    val info: BankingModelInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val canSwitch: Boolean = false
)

sealed class BankingModelEvent {
    object LoadInfo : BankingModelEvent()
    data class SwitchModel(val newModel: BankingModel) : BankingModelEvent()
    object ClearError : BankingModelEvent()
    object ClearSuccess : BankingModelEvent()
}
```

### State Updates

```kotlin
fun reduce(event: BankingModelEvent): BankingModelState {
    return when (event) {
        is BankingModelEvent.LoadInfo -> {
            copy(isLoading = true, error = null)
        }
        is BankingModelEvent.SwitchModel -> {
            copy(isLoading = true, error = null, successMessage = null)
        }
        is BankingModelEvent.ClearError -> {
            copy(error = null)
        }
        is BankingModelEvent.ClearSuccess -> {
            copy(successMessage = null)
        }
    }
}
```

---

## Testing Checklist

### Functional Tests

- [ ] Headquarters VENDOR_ADMIN can view banking model info
- [ ] Headquarters VENDOR_ADMIN can switch from SHARED to INDIVIDUAL
- [ ] Headquarters VENDOR_ADMIN can switch from INDIVIDUAL to SHARED
- [ ] Non-headquarters VENDOR_ADMIN cannot switch (error shown)
- [ ] VENDOR (worker) cannot access banking model endpoints
- [ ] Error messages are displayed correctly
- [ ] Success messages are displayed after switch
- [ ] Banking model info refreshes after switch

### UI Tests

- [ ] Banking model section is visible for headquarters
- [ ] Banking model section shows info-only for non-headquarters
- [ ] Banking model section is hidden for workers
- [ ] Confirmation dialog appears before switch
- [ ] Loading indicator shows during API calls
- [ ] Current model is highlighted correctly

### Edge Cases

- [ ] Network failure handling
- [ ] Invalid token handling
- [ ] Already set to target model (no-op)
- [ ] Multiple rapid switch attempts
- [ ] App backgrounded during switch

### Integration Tests

- [ ] API calls use correct authentication token
- [ ] Request body format is correct
- [ ] Response parsing handles all fields
- [ ] Error responses are parsed correctly

---

## Additional Notes

### Notification Handling

When a banking model is switched, location managers receive:
1. **Email notification** (sent immediately)
2. **In-app/push notification** (via notification service)

The Android app should handle these notifications appropriately:
- Show notification badge if banking model changed
- Navigate to banking model screen when notification tapped
- Refresh banking model info when app resumes

### Offer Invalidation

When switching to INDIVIDUAL:
- All offers are automatically invalidated
- Locations must reactivate offers after setting up payment accounts
- Consider showing a warning in the offers list for affected locations

### Best Practices

1. **Cache banking model info** to reduce API calls
2. **Refresh on app resume** to get latest model
3. **Show clear warnings** before switching to INDIVIDUAL
4. **Provide headquarters contact** for non-headquarters users
5. **Handle errors gracefully** with user-friendly messages
6. **Log errors** for debugging purposes

---

## Summary

The banking model switching feature allows headquarters VENDOR_ADMINs to manage how chain locations handle payments. Key implementation points:

1. ✅ Check if user is headquarters before showing switch UI
2. ✅ Use proper authentication (JWT Bearer token)
3. ✅ Handle all error scenarios gracefully
4. ✅ Show confirmation dialog, especially for INDIVIDUAL switch
5. ✅ Provide clear feedback on success/failure
6. ✅ Refresh banking model info after switch
7. ✅ Show appropriate UI based on user permissions

**Remember**: Only Headquarters VENDOR_ADMIN can switch banking models. All other users should see read-only information or be informed to contact headquarters.

