# Android Password Reset with Email Verification Implementation Prompt

## Overview
Implement the two-step password reset feature in the Android mobile application. This feature requires users to submit their email and new password, then verify via email link before the password is actually changed. This follows security best practices by requiring explicit email confirmation.

## New Password Reset Flow

### Step 1: Request Password Reset (Submit Email + New Password)
User enters their email and desired new password, then receives a verification email.

### Step 2: Confirm Password Reset (Click Email Link)
User clicks the verification link in the email, which confirms and applies the password change.

## API Endpoint Details

### Step 1: Request Password Reset

**Endpoint:**
```
POST http://localhost:8080/auth/forgot-password
```

**Note:** Replace `localhost:8080` with your actual API gateway URL in production.

**Request Details:**
- **Method:** POST
- **Content-Type:** application/json
- **Authentication:** Not required (public endpoint)
- **Request Body:**
```json
{
  "email": "user@example.com",
  "newPassword": "newPassword123"
}
```

**Response Details:**

**Success Response (200 OK):**
```
"Verification link sent to your email. Please click the link to confirm the password reset."
```

**Error Responses:**
- **404 NOT FOUND:** `"User not found with email: {email}"`
- **400 BAD REQUEST:** `"Password must be at least 6 characters long"` (or other validation errors)
- **500 INTERNAL SERVER ERROR:** `"Failed to send password reset verification link"`

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
  - `"Invalid password reset token"`
  - `"Password reset token has expired"`
  - `"Password reset token is invalid - no password reset request found"`

## Implementation Requirements

### 1. Request Password Reset Screen

Create a "Request Password Reset" screen with the following elements:

- **Email Input Field:**
  - Material Design TextInputLayout with TextInputEditText
  - Email validation (valid email format)
  - Placeholder: "Enter your email address"
  - Input type: `InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS`

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

- **Submit Button:**
  - Text: "Request Password Reset" or "Send Verification Link"
  - Disable while request is in progress
  - Show loading indicator during API call
  - Disable if validation fails

- **Back to Login Link:**
  - Navigate back to login screen

- **Success Message:**
  - Display after successful submission
  - Message: "Verification link has been sent to your email. Please check your inbox and click the link to confirm the password reset."
  - Optionally show the email address
  - Instructions to check spam folder

### 2. Network Implementation

**Create a Retrofit Interface:**
```kotlin
interface AuthApiService {
    @POST("auth/forgot-password")
    suspend fun requestPasswordReset(
        @Body request: PasswordResetRequest
    ): Response<String>
    
    @GET("auth/reset-password")
    suspend fun confirmPasswordReset(
        @Query("token") token: String,
        @Header("Accept") accept: String = "application/json"
    ): Response<PasswordResetConfirmationResponse>
}

data class PasswordResetRequest(
    val email: String,
    val newPassword: String
)

data class PasswordResetConfirmationResponse(
    val success: Boolean,
    val message: String,
    val email: String?
)
```

**Create a Repository:**
```kotlin
class AuthRepository(private val apiService: AuthApiService) {
    suspend fun requestPasswordReset(email: String, newPassword: String): Result<String> {
        return try {
            val request = PasswordResetRequest(email, newPassword)
            val response = apiService.requestPasswordReset(request)
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
class PasswordResetViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PasswordResetUiState>(PasswordResetUiState.Idle)
    val uiState: StateFlow<PasswordResetUiState> = _uiState.asStateFlow()
    
    private val _passwordMatch = MutableStateFlow(true)
    val passwordMatch: StateFlow<Boolean> = _passwordMatch.asStateFlow()
    
    fun requestPasswordReset(email: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            // Validate inputs
            if (!isValidEmail(email)) {
                _uiState.value = PasswordResetUiState.Error("Please enter a valid email address")
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
            
            authRepository.requestPasswordReset(email, newPassword)
                .onSuccess { message ->
                    _uiState.value = PasswordResetUiState.Success(message)
                }
                .onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("not found") == true -> 
                            "No account found with this email address"
                        exception.message?.contains("at least 6") == true -> 
                            "Password must be at least 6 characters long"
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
}
```

### 5. Request Password Reset Screen (Compose)

```kotlin
@Composable
fun RequestPasswordResetScreen(
    viewModel: PasswordResetViewModel = viewModel(),
    onBackToLogin: () -> Unit,
    onVerificationSent: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val passwordMatch by viewModel.passwordMatch.collectAsState()
    
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
            text = "Enter your email and new password. A verification link will be sent to your email.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )
        
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
            enabled = uiState !is PasswordResetUiState.Loading
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
                viewModel.requestPasswordReset(email, newPassword, confirmPassword)
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
                Text("Send Verification Link")
            }
        }
        
        TextButton(onClick = onBackToLogin) {
            Text("Back to Login")
        }
        
        // Handle UI state
        when (uiState) {
            is PasswordResetUiState.Success -> {
                LaunchedEffect(Unit) {
                    // Show success dialog
                    // Navigate after delay
                    delay(2000)
                    onVerificationSent()
                }
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

### 6. Deep Link Handling for Email Verification

**AndroidManifest.xml:**
```xml
<activity
    android:name=".ui.auth.ConfirmPasswordResetActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="yourdomain.com"
            android:pathPrefix="/auth/reset-password" />
        <data
            android:scheme="http"
            android:host="localhost"
            android:port="8080"
            android:pathPrefix="/auth/reset-password" />
    </intent-filter>
</activity>
```

**Activity Implementation:**
```kotlin
class ConfirmPasswordResetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val token = intent.data?.getQueryParameter("token")
        
        setContent {
            YourAppTheme {
                ConfirmPasswordResetScreen(
                    token = token,
                    onPasswordResetConfirmed = {
                        // Navigate to login
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
```

### 7. Confirm Password Reset Screen (Compose)

```kotlin
@Composable
fun ConfirmPasswordResetScreen(
    token: String?,
    viewModel: PasswordResetViewModel = viewModel(),
    onPasswordResetConfirmed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Auto-confirm when screen loads with valid token
    LaunchedEffect(token) {
        if (token != null) {
            viewModel.confirmPasswordReset(token)
        }
    }
    
    when {
        token == null -> {
            // Show error - invalid link
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Invalid Reset Link",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "The password reset link is invalid or missing.",
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        uiState is PasswordResetUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Confirming password reset...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        
        uiState is PasswordResetUiState.Confirmed -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Password Reset Successful!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    text = "Your password has been successfully reset.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = onPasswordResetConfirmed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Login")
                }
            }
        }
        
        uiState is PasswordResetUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Password Reset Failed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = onPasswordResetConfirmed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Login")
                }
            }
        }
        
        else -> {
            // Initial state
            CircularProgressIndicator()
        }
    }
}
```

### 8. Error Handling Best Practices

1. **Email Validation:**
   - Validate email format before API call
   - Show inline validation errors
   - Prevent API call if validation fails

2. **Password Validation:**
   - Minimum 6 characters
   - Real-time validation feedback
   - Password match validation
   - Show clear error messages

3. **Network Errors:**
   - Check for internet connectivity
   - Show appropriate message: "Please check your internet connection"
   - Handle timeout errors gracefully
   - Provide retry option

4. **API Errors:**
   - Parse error response body
   - Show user-friendly messages:
     - 404: "No account found with this email address"
     - 400: Show validation error message
     - 500: "Something went wrong. Please try again later"

5. **Token Errors:**
   - Invalid token: "This verification link is invalid. Please request a new password reset."
   - Expired token: "This verification link has expired. Please request a new password reset."
   - Missing token: "Invalid reset link. Please request a new password reset."

### 9. Testing Checklist

**Request Password Reset:**
- [ ] Test with valid email and password
- [ ] Test with invalid email format
- [ ] Test with password less than 6 characters
- [ ] Test with non-matching passwords
- [ ] Test with non-existent email (404 error)
- [ ] Test network error handling
- [ ] Test timeout scenarios
- [ ] Test button disabled states
- [ ] Test success message display
- [ ] Test navigation after success

**Confirm Password Reset:**
- [ ] Test with valid token (from email link)
- [ ] Test with invalid token
- [ ] Test with expired token
- [ ] Test deep link handling
- [ ] Test auto-confirmation on screen load
- [ ] Test success screen display
- [ ] Test error screen display
- [ ] Test navigation to login after success
- [ ] Test navigation to login after error

### 10. Security Considerations

1. **Password Handling:**
   - Use secure text input fields
   - Don't log passwords
   - Clear password fields after successful submission
   - Don't store passwords in memory unnecessarily

2. **Token Handling:**
   - Don't log tokens
   - Clear token from memory after use
   - Don't store tokens persistently
   - Handle malicious or malformed tokens

3. **User Feedback:**
   - Show same success message regardless of email existence (security best practice)
   - Don't reveal if email is registered or not in error messages
   - Provide clear instructions for next steps

4. **Rate Limiting:**
   - Consider client-side rate limiting
   - Show appropriate message if user tries too many times
   - Disable button temporarily after submission

### 11. User Flow Diagram

```
User opens "Forgot Password" screen
    ↓
User enters email and new password
    ↓
User confirms password match
    ↓
Submit request → POST /auth/forgot-password
    ↓
Success → Show message: "Verification link sent"
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

### 12. Additional Features (Optional)

1. **Resend Verification Email:**
   - Add "Resend Email" button after submission
   - Implement cooldown period (e.g., 60 seconds)
   - Show countdown timer

2. **Password Strength Indicator:**
   - Visual indicator (weak/medium/strong)
   - Provide feedback on password requirements
   - Real-time strength calculation

3. **Email Auto-fill:**
   - Pre-fill email if user is logged in
   - Support Android's Autofill framework

4. **Accessibility:**
   - Add content descriptions for screen readers
   - Ensure proper focus order
   - Support keyboard navigation
   - Support screen readers

## Important Notes

- **Two-Step Process:** Password is NOT changed until user clicks the verification link
- **Universal Endpoint:** Works for both users and vendors - no need to distinguish
- **Email Verification Required:** User must have access to email to complete reset
- **Token Expiry:** Verification links expire after 24 hours
- **Security:** This flow prevents unauthorized password changes

## Example API Calls

**Request Password Reset:**
```kotlin
val request = PasswordResetRequest(
    email = "user@example.com",
    newPassword = "newPassword123"
)
val response = apiService.requestPasswordReset(request)
```

**Confirm Password Reset:**
```kotlin
val response = apiService.confirmPasswordReset(token)
// Response: { "success": true, "message": "...", "email": "user@example.com" }
```

## Integration with Existing Features

- This replaces the old single-step password reset flow
- The change-password endpoint (authenticated) remains unchanged
- Both flows work universally for users and vendors
- All password changes sync across databases via Kafka events

