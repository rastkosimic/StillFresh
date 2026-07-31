# Android Authenticated Password Reset Implementation Prompt

## Overview
Implement the authenticated password reset feature in the Android mobile application. This feature allows logged-in users or vendors to change their password through a two-step verification process. After submitting the request, the user is automatically logged out for security, and must verify via email link before the password is actually changed.

## Password Reset Flow for Authenticated Users

### Step 1: Request Password Reset (Submit Email + New Password)
Authenticated user/vendor enters their email and desired new password (twice for confirmation), then receives a verification email. User is automatically logged out after submission.

### Step 2: Confirm Password Reset (Click Email Link)
User clicks the verification link in the email, which confirms and applies the password change.

## API Endpoint Details

### Step 1: Request Password Reset (Authenticated)

**Endpoint:**
```
POST http://localhost:8080/auth/change-password
```

**Note:** Replace `localhost:8080` with your actual API gateway URL in production.

**Request Details:**
- **Method:** POST
- **Content-Type:** application/json
- **Authentication:** Required (Bearer token in Authorization header)
- **Request Body:**
```json
{
  "email": "user@example.com",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Response Details:**

**Success Response (200 OK):**
```
"Verification link sent to your email. Please check your email and click the link to confirm the password reset. You have been logged out for security."
```

**Error Responses:**
- **400 BAD REQUEST:** 
  - `"Email does not match your account email"` - Email provided doesn't match the authenticated user's email
  - `"New password and confirm password do not match"` - Passwords don't match
  - `"Password must be at least 6 characters long"` - Password too short
- **401 UNAUTHORIZED:** User is not authenticated or token is invalid
- **500 INTERNAL SERVER ERROR:** `"Failed to send password reset verification link"`

**Important:** After successful submission, the user's authentication token is invalidated and they are logged out. The app must handle this logout and redirect to the login screen.

### Step 2: Confirm Password Reset (via Email Link)

**Endpoint:**
```
GET http://localhost:8080/auth/reset-password?token={token}
```

**Request Details:**
- **Method:** GET
- **Authentication:** Not required (public endpoint)
- **Query Parameter:**
  - `token` (String, required): The verification token from the email link
- **Optional Header:**
  - `Accept: application/json` - To receive JSON response instead of HTML

**Response Details:**

**Success Response (200 OK):**

**JSON Response (if Accept: application/json header is present):**
```json
{
  "success": true,
  "message": "Password reset successfully confirmed",
  "email": "user@example.com"
}
```

**HTML Response (if opened in browser):**
- Success page with confirmation message
- Link to login page

**Error Responses:**
- **400 BAD REQUEST:** 
  - `"Invalid password reset token"` - Token doesn't exist
  - `"Password reset token has expired"` - Token expired (24 hours)
  - `"Password reset token is invalid - no password reset request found"` - Token is incomplete

## Implementation Requirements

### 1. Authenticated Password Reset Screen

Create a "Change Password" or "Reset Password" screen accessible from the user/vendor profile or settings screen with the following elements:

- **Email Input Field:**
  - Material Design TextInputLayout with TextInputEditText
  - Pre-filled with the authenticated user's email (read-only or editable)
  - Email validation (valid email format)
  - Placeholder: "Enter your email address"
  - Input type: `InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS`
  - **Note:** The email must match the authenticated user's email

- **New Password Input Field:**
  - Material Design TextInputLayout with TextInputEditText
  - Input type: Password (with show/hide toggle)
  - Minimum 6 characters validation
  - Placeholder: "Enter new password"
  - Helper text: "Password must be at least 6 characters"
  - Real-time validation feedback

- **Confirm Password Input Field:**
  - Same as new password field
  - Validate that passwords match
  - Show error if passwords don't match
  - Placeholder: "Confirm new password"

- **Submit Button:**
  - Text: "Reset Password" or "Change Password"
  - Disable while request is in progress
  - Show loading indicator during API call
  - Disable if validation fails

- **Warning Message:**
  - Display before submission: "You will be logged out after submitting this request. Please check your email to confirm the password reset."
  - Make it clear that logout is automatic

- **Success Message:**
  - Display after successful submission
  - Message: "Verification link has been sent to your email. You have been logged out. Please check your inbox and click the link to confirm the password reset."
  - Instructions to check spam folder
  - Auto-navigate to login screen after showing message

### 2. Network Implementation

**Update Retrofit Interface:**
```kotlin
interface AuthApiService {
    @POST("auth/change-password")
    suspend fun requestAuthenticatedPasswordReset(
        @Header("Authorization") token: String,
        @Body request: AuthenticatedPasswordResetRequest
    ): Response<String>
    
    @GET("auth/reset-password")
    suspend fun confirmPasswordReset(
        @Query("token") token: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<PasswordResetConfirmationResponse>
}

data class AuthenticatedPasswordResetRequest(
    val email: String,
    val newPassword: String,
    val confirmPassword: String
)

data class PasswordResetConfirmationResponse(
    val success: Boolean,
    val message: String,
    val email: String?
)
```

**Update Repository:**
```kotlin
class AuthRepository(private val apiService: AuthApiService) {
    suspend fun requestAuthenticatedPasswordReset(
        token: String,
        email: String,
        newPassword: String,
        confirmPassword: String
    ): Result<String> {
        return try {
            val request = AuthenticatedPasswordResetRequest(
                email = email,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            val response = apiService.requestAuthenticatedPasswordReset(
                "Bearer $token",
                request
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: "Success")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun confirmPasswordReset(token: String): Result<PasswordResetConfirmationResponse> {
        return try {
            val response = apiService.confirmPasswordReset(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 3. ViewModel Implementation

```kotlin
class AuthenticatedPasswordResetViewModel(
    private val authRepository: AuthRepository,
    private val authTokenManager: AuthTokenManager, // Manages stored auth token
    private val userManager: UserManager // Manages current user info
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PasswordResetUiState>(PasswordResetUiState.Idle)
    val uiState: StateFlow<PasswordResetUiState> = _uiState.asStateFlow()
    
    private val _passwordMatch = MutableStateFlow(true)
    val passwordMatch: StateFlow<Boolean> = _passwordMatch.asStateFlow()
    
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()
    
    init {
        // Pre-fill email from current user
        _userEmail.value = userManager.getCurrentUserEmail()
    }
    
    fun requestPasswordReset(
        email: String,
        newPassword: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            // Validate inputs
            if (!isValidEmail(email)) {
                _uiState.value = PasswordResetUiState.Error("Please enter a valid email address")
                return@launch
            }
            
            // Verify email matches authenticated user's email
            val currentUserEmail = userManager.getCurrentUserEmail()
            if (email != currentUserEmail) {
                _uiState.value = PasswordResetUiState.Error("Email does not match your account email")
                return@launch
            }
            
            if (newPassword.length < 6) {
                _uiState.value = PasswordResetUiState.Error("Password must be at least 6 characters long")
                return@launch
            }
            
            if (newPassword != confirmPassword) {
                _passwordMatch.value = false
                _uiState.value = PasswordResetUiState.Error("Passwords do not match")
                return@launch
            }
            
            _passwordMatch.value = true
            _uiState.value = PasswordResetUiState.Loading
            
            // Get auth token
            val token = authTokenManager.getToken()
            if (token == null) {
                _uiState.value = PasswordResetUiState.Error("You are not authenticated. Please log in again.")
                return@launch
            }
            
            authRepository.requestAuthenticatedPasswordReset(
                token = token,
                email = email,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
                .onSuccess { message ->
                    // Clear auth token and user session (user is logged out)
                    authTokenManager.clearToken()
                    userManager.clearUser()
                    
                    _uiState.value = PasswordResetUiState.Success(message)
                }
                .onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("does not match") == true -> 
                            "Email does not match your account email"
                        exception.message?.contains("do not match") == true -> 
                            "New password and confirm password do not match"
                        exception.message?.contains("at least 6") == true -> 
                            "Password must be at least 6 characters long"
                        exception.message?.contains("401") == true || 
                        exception.message?.contains("Unauthorized") == true -> 
                            "Your session has expired. Please log in again."
                        else -> 
                            exception.message ?: "Failed to send verification link"
                    }
                    _uiState.value = PasswordResetUiState.Error(errorMessage)
                }
        }
    }
    
    fun confirmPasswordReset(token: String) {
        viewModelScope.launch {
            _uiState.value = PasswordResetUiState.Loading
            
            authRepository.confirmPasswordReset(token)
                .onSuccess { response ->
                    _uiState.value = PasswordResetUiState.Confirmed(response)
                }
                .onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("Invalid") == true -> 
                            "Invalid or expired verification link. Please request a new password reset."
                        exception.message?.contains("expired") == true -> 
                            "This verification link has expired. Please request a new password reset."
                        else -> 
                            exception.message ?: "Failed to confirm password reset"
                    }
                    _uiState.value = PasswordResetUiState.Error(errorMessage)
                }
        }
    }
    
    fun validatePasswordsMatch(newPassword: String, confirmPassword: String) {
        _passwordMatch.value = newPassword == confirmPassword || confirmPassword.isEmpty()
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
```

### 4. UI State Sealed Class

```kotlin
sealed class PasswordResetUiState {
    object Idle : PasswordResetUiState()
    object Loading : PasswordResetUiState()
    data class Success(val message: String) : PasswordResetUiState()
    data class Confirmed(val response: PasswordResetConfirmationResponse) : PasswordResetUiState()
    data class Error(val message: String) : PasswordResetUiState()
    object LoggedOut : PasswordResetUiState() // User has been logged out
}
```

### 5. Authenticated Password Reset Screen (Compose)

```kotlin
@Composable
fun AuthenticatedPasswordResetScreen(
    viewModel: AuthenticatedPasswordResetViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val passwordMatch by viewModel.passwordMatch.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    
    // Pre-fill email when available
    LaunchedEffect(userEmail) {
        if (userEmail != null && email.isEmpty()) {
            email = userEmail!!
        }
    }
    
    // Handle logout after success
    LaunchedEffect(uiState) {
        if (uiState is PasswordResetUiState.Success) {
            // Show success message, then navigate to login
            delay(3000) // Show message for 3 seconds
            onNavigateToLogin()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Reset Password",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enter your email and new password. You will be logged out after submission and must verify via email to complete the reset.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Warning banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You will be logged out after submitting this request.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            placeholder = { Text("Enter your email address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            enabled = uiState !is PasswordResetUiState.Loading,
            readOnly = userEmail != null && email == userEmail // Read-only if pre-filled
        )
        
        OutlinedTextField(
            value = newPassword,
            onValueChange = { 
                newPassword = it
                viewModel.validatePasswordsMatch(newPassword, confirmPassword)
            },
            label = { Text("New Password") },
            placeholder = { Text("Enter new password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            singleLine = true,
            enabled = uiState !is PasswordResetUiState.Loading,
            isError = newPassword.isNotBlank() && newPassword.length < 6
        )
        
        if (newPassword.isNotBlank() && newPassword.length < 6) {
            Text(
                text = "Password must be at least 6 characters",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp)
            )
        }
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it
                viewModel.validatePasswordsMatch(newPassword, confirmPassword)
            },
            label = { Text("Confirm Password") },
            placeholder = { Text("Confirm new password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                    )
                }
            },
            singleLine = true,
            enabled = uiState !is PasswordResetUiState.Loading,
            isError = !passwordMatch && confirmPassword.isNotBlank()
        )
        
        if (!passwordMatch && confirmPassword.isNotBlank()) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp)
            )
        }
        
        Button(
            onClick = { 
                showWarningDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = uiState !is PasswordResetUiState.Loading && 
                     email.isNotBlank() &&
                     newPassword.length >= 6 && 
                     passwordMatch &&
                     confirmPassword.isNotBlank()
        ) {
            if (uiState is PasswordResetUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Reset Password")
            }
        }
        
        TextButton(onClick = onBack) {
            Text("Cancel")
        }
        
        // Warning dialog
        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = { showWarningDialog = false },
                title = { Text("Confirm Password Reset") },
                text = {
                    Text(
                        "You will be logged out immediately after submitting this request. " +
                        "You must check your email and click the verification link to complete the password reset. " +
                        "Do you want to continue?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showWarningDialog = false
                            viewModel.requestPasswordReset(email, newPassword, confirmPassword)
                        }
                    ) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWarningDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Handle UI state
        when (uiState) {
            is PasswordResetUiState.Success -> {
                LaunchedEffect(Unit) {
                    // Show success snackbar
                }
                // Success message will be shown, then navigate to login
            }
            is PasswordResetUiState.Error -> {
                LaunchedEffect(uiState.message) {
                    // Show error snackbar
                }
            }
            else -> {}
        }
    }
}
```

### 6. Handle Logout After Submission

**Important:** After successful password reset request, the user is automatically logged out. The app must:

1. **Clear Authentication State:**
   - Remove stored auth token
   - Clear user session data
   - Clear any cached user information

2. **Navigate to Login:**
   - Automatically navigate to login screen
   - Show success message before navigation
   - Optionally show a dialog explaining the logout

3. **Handle Token Invalidation:**
   - All subsequent API calls will fail with 401
   - App should handle 401 errors globally and redirect to login

**Example Logout Handler:**
```kotlin
class AuthTokenManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }
    
    fun clearToken() {
        sharedPreferences.edit().remove("auth_token").apply()
    }
    
    fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }
}
```

### 7. Deep Link Handling for Email Verification

The email verification link handling is the same as the unauthenticated flow. See `ANDROID_PASSWORD_RESET_VERIFICATION_FLOW_PROMPT.md` for details on deep link handling.

### 8. Error Handling Best Practices

1. **Email Validation:**
   - Pre-fill with authenticated user's email
   - Validate that email matches authenticated user
   - Show clear error if email doesn't match

2. **Password Validation:**
   - Minimum 6 characters
   - Real-time validation feedback
   - Password match validation
   - Show clear error messages

3. **Authentication Errors:**
   - Handle 401 errors (token invalidated)
   - Redirect to login if authentication fails
   - Show appropriate message: "Your session has expired"

4. **Network Errors:**
   - Check for internet connectivity
   - Show appropriate message: "Please check your internet connection"
   - Handle timeout errors gracefully
   - Provide retry option

5. **API Errors:**
   - Parse error response body
   - Show user-friendly messages:
     - 400: Show validation error message
     - 401: "Your session has expired. Please log in again."
     - 500: "Something went wrong. Please try again later"

### 9. Testing Checklist

**Request Password Reset (Authenticated):**
- [ ] Test with valid email (matching authenticated user) and password
- [ ] Test with email that doesn't match authenticated user
- [ ] Test with invalid email format
- [ ] Test with password less than 6 characters
- [ ] Test with non-matching passwords
- [ ] Test with expired/invalid auth token (401 error)
- [ ] Test network error handling
- [ ] Test timeout scenarios
- [ ] Test button disabled states
- [ ] Test success message display
- [ ] Test automatic logout after success
- [ ] Test navigation to login after logout
- [ ] Test token invalidation (subsequent API calls fail)
- [ ] Test warning dialog display

**Confirm Password Reset:**
- [ ] Test with valid token (from email link)
- [ ] Test with invalid token
- [ ] Test with expired token
- [ ] Test deep link handling
- [ ] Test auto-confirmation on screen load
- [ ] Test success screen display
- [ ] Test error screen display
- [ ] Test navigation to login after success

### 10. Security Considerations

1. **Password Handling:**
   - Use secure text input fields
   - Don't log passwords
   - Clear password fields after successful submission
   - Don't store passwords in memory unnecessarily

2. **Token Handling:**
   - Don't log tokens
   - Clear token from memory after logout
   - Invalidate token immediately after password reset request
   - Handle token invalidation gracefully

3. **User Feedback:**
   - Show clear warning about logout
   - Provide clear instructions for next steps
   - Don't reveal sensitive information in error messages

4. **Session Management:**
   - Immediately invalidate session after password reset request
   - Clear all user data from local storage
   - Prevent any further authenticated API calls
   - Redirect to login screen

### 11. User Flow Diagram

```
User navigates to "Change Password" from profile/settings
    ↓
Screen loads with pre-filled email (from authenticated user)
    ↓
User enters new password (twice for confirmation)
    ↓
User clicks "Reset Password"
    ↓
Warning dialog: "You will be logged out"
    ↓
User confirms → POST /auth/change-password (with auth token)
    ↓
Success → Token invalidated → User logged out
    ↓
Show success message: "Verification link sent. You have been logged out."
    ↓
Auto-navigate to Login screen (after 3 seconds)
    ↓
User receives email with verification link
    ↓
User clicks link → Deep link opens app
    ↓
App extracts token from URL
    ↓
Auto-call → GET /auth/reset-password?token={token}
    ↓
Success → Show success screen → Navigate to Login
Error → Show error screen → Option to request new reset
```

### 12. Integration Points

1. **Profile/Settings Screen:**
   - Add "Change Password" or "Reset Password" option
   - Navigate to AuthenticatedPasswordResetScreen
   - Show appropriate icon/button

2. **Navigation:**
   - Handle back navigation (cancel password reset)
   - Navigate to login after logout
   - Handle deep links for email verification

3. **Authentication State:**
   - Monitor authentication state changes
   - Handle automatic logout
   - Clear all user-related data

4. **Global Error Handler:**
   - Handle 401 errors globally
   - Redirect to login on authentication failure
   - Show appropriate messages

## Important Notes

- **Two-Step Process:** Password is NOT changed until user clicks the verification link
- **Automatic Logout:** User is logged out immediately after submitting the request
- **Email Verification Required:** User must have access to email to complete reset
- **Token Expiry:** Verification links expire after 24 hours
- **Security:** This flow prevents unauthorized password changes and ensures user is logged out for security
- **Universal Endpoint:** Works for both users and vendors - no need to distinguish
- **Email Must Match:** The email provided must match the authenticated user's email

## Example API Calls

**Request Password Reset (Authenticated):**
```kotlin
val token = authTokenManager.getToken() // "Bearer eyJhbGciOiJIUzI1NiIs..."
val request = AuthenticatedPasswordResetRequest(
    email = "user@example.com",
    newPassword = "newPassword123",
    confirmPassword = "newPassword123"
)
val response = apiService.requestAuthenticatedPasswordReset(token, request)
// Response: "Verification link sent to your email. Please check your email and click the link to confirm the password reset. You have been logged out for security."
// After this, token is invalidated and user must log in again
```

**Confirm Password Reset:**
```kotlin
val response = apiService.confirmPasswordReset(token)
// Response: { "success": true, "message": "...", "email": "user@example.com" }
```

## Differences from Unauthenticated Flow

1. **Authentication Required:** Must be logged in to access this flow
2. **Email Pre-filled:** Email is automatically filled from authenticated user
3. **Email Validation:** Email must match authenticated user's email
4. **Automatic Logout:** User is logged out immediately after request
5. **Token Invalidation:** Auth token is invalidated server-side
6. **Session Clearing:** All local session data must be cleared
7. **Navigation:** Must navigate to login screen after logout

## Integration with Existing Features

- This complements the unauthenticated password reset flow
- Both flows use the same email verification endpoint
- Both flows work universally for users and vendors
- All password changes sync across databases via Kafka events
- The authenticated flow adds automatic logout for security

