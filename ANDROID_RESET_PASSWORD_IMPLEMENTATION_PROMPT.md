# Android Reset Password Implementation Prompt

## Overview
Implement the "Reset Password" feature in the Android mobile application. This feature allows users (both regular users and vendors) to reset their password using a token received via email after requesting a password reset.

## API Endpoint Details

### Endpoint
```
POST http://localhost:8080/auth/reset-password?token={token}
```

**Note:** Replace `localhost:8080` with your actual API gateway URL in production.

### Request Details
- **Method:** POST
- **Content-Type:** application/json
- **Authentication:** Not required (public endpoint)
- **Query Parameter:**
  - `token` (String, required): The password reset token received via email link

### Request Body
The request body should be a **plain string** (not a JSON object) containing the new password:

```
"newPassword123"
```

**Important:** The API expects a plain string in the request body, not a JSON object like `{"password": "newPassword123"}`.

### Response Details

**Success Response (200 OK):**
```
"Password reset successfully"
```

**Error Responses:**
- **400 BAD REQUEST:** 
  - `"Invalid password reset token"`
  - `"Token has expired"`
- **500 INTERNAL SERVER ERROR:** `"Failed to reset password"`

## Token Extraction from Deep Link

The reset password token will be received via a deep link in the email. The email link format is:
```
http://localhost:8080/auth/reset-password?token={token}
```

For Android, you should configure a deep link to handle this URL pattern.

## Implementation Requirements

### 1. Deep Link Configuration

**AndroidManifest.xml:**
```xml
<activity
    android:name=".ui.auth.ResetPasswordActivity"
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

**Extract token from intent:**
```kotlin
val token = intent.data?.getQueryParameter("token")
```

### 2. UI/UX Design

Create a "Reset Password" screen with the following elements:

- **New Password Input Field:**
  - Use Material Design TextInputLayout with TextInputEditText
  - Input type: `InputType.TYPE_TEXT_VARIATION_PASSWORD` or `InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD`
  - Show/hide password toggle (eye icon)
  - Add password strength indicator (optional but recommended)
  - Minimum 6 characters validation
  - Placeholder: "Enter new password"
  - Helper text: "Password must be at least 6 characters"

- **Confirm Password Input Field:**
  - Same as above
  - Placeholder: "Confirm new password"
  - Validate that passwords match
  - Show error if passwords don't match

- **Reset Password Button:**
  - Material Design button
  - Text: "Reset Password" or "Set New Password"
  - Disable button while request is in progress
  - Show loading indicator during API call
  - Disable if passwords don't match or validation fails

- **Success/Error Messages:**
  - Display success message after successful reset
  - Navigate to login screen after success
  - Show error messages for invalid/expired tokens
  - Handle network errors gracefully

### 3. Network Implementation

**Create a Retrofit Interface:**
```kotlin
interface AuthApiService {
    @POST("auth/reset-password")
    @Headers("Content-Type: application/json")
    suspend fun resetPassword(
        @Query("token") token: String,
        @Body newPassword: String  // Plain string, not JSON object
    ): Response<String>
}
```

**Alternative using RequestBody:**
```kotlin
@POST("auth/reset-password")
suspend fun resetPassword(
    @Query("token") token: String,
    @Body newPassword: RequestBody
): Response<String>
```

**Create a Repository:**
```kotlin
class AuthRepository(private val apiService: AuthApiService) {
    suspend fun resetPassword(token: String, newPassword: String): Result<String> {
        return try {
            // Convert string to RequestBody for Retrofit
            val requestBody = newPassword.toRequestBody("application/json".toMediaType())
            
            val response = apiService.resetPassword(token, requestBody)
            if (response.isSuccessful) {
                Result.success(response.body() ?: "Password reset successfully")
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

**Note:** If using plain string body, you may need a custom converter. Here's an alternative approach:

```kotlin
// Using OkHttp directly or custom converter
val json = "\"$newPassword\""  // Wrap in quotes for JSON string
val requestBody = json.toRequestBody("application/json".toMediaType())
```

### 4. ViewModel Implementation

```kotlin
class ResetPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ResetPasswordUiState>(ResetPasswordUiState.Idle)
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()
    
    private val _passwordMatch = MutableStateFlow(true)
    val passwordMatch: StateFlow<Boolean> = _passwordMatch.asStateFlow()
    
    fun resetPassword(token: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            // Validate passwords
            if (newPassword.length < 6) {
                _uiState.value = ResetPasswordUiState.Error("Password must be at least 6 characters")
                return@launch
            }
            
            if (newPassword != confirmPassword) {
                _passwordMatch.value = false
                _uiState.value = ResetPasswordUiState.Error("Passwords do not match")
                return@launch
            }
            
            _passwordMatch.value = true
            _uiState.value = ResetPasswordUiState.Loading
            
            authRepository.resetPassword(token, newPassword)
                .onSuccess { message ->
                    _uiState.value = ResetPasswordUiState.Success(message)
                }
                .onFailure { exception ->
                    val errorMessage = when {
                        exception.message?.contains("Invalid") == true -> 
                            "Invalid or expired reset link. Please request a new password reset."
                        exception.message?.contains("expired") == true -> 
                            "This reset link has expired. Please request a new password reset."
                        else -> 
                            exception.message ?: "Failed to reset password. Please try again."
                    }
                    _uiState.value = ResetPasswordUiState.Error(errorMessage)
                }
        }
    }
    
    fun validatePasswordsMatch(password: String, confirmPassword: String) {
        _passwordMatch.value = password == confirmPassword || confirmPassword.isEmpty()
    }
}
```

### 5. UI State Sealed Class

```kotlin
sealed class ResetPasswordUiState {
    object Idle : ResetPasswordUiState()
    object Loading : ResetPasswordUiState()
    data class Success(val message: String) : ResetPasswordUiState()
    data class Error(val message: String) : ResetPasswordUiState()
}
```

### 6. Compose UI Implementation

```kotlin
@Composable
fun ResetPasswordScreen(
    token: String?,
    viewModel: ResetPasswordViewModel = viewModel(),
    onPasswordResetSuccess: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val passwordMatch by viewModel.passwordMatch.collectAsState()
    
    // Check if token is available
    if (token == null) {
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
        return
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
            text = "Enter your new password below.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            textAlign = TextAlign.Center
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
            enabled = uiState !is ResetPasswordUiState.Loading,
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
            enabled = uiState !is ResetPasswordUiState.Loading,
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
            onClick = { viewModel.resetPassword(token, newPassword, confirmPassword) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = uiState !is ResetPasswordUiState.Loading && 
                     newPassword.length >= 6 && 
                     passwordMatch &&
                     confirmPassword.isNotBlank()
        ) {
            if (uiState is ResetPasswordUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Reset Password")
            }
        }
        
        // Handle UI state
        when (uiState) {
            is ResetPasswordUiState.Success -> {
                LaunchedEffect(Unit) {
                    // Show success dialog
                    // Navigate to login after delay
                    delay(2000)
                    onPasswordResetSuccess()
                }
            }
            is ResetPasswordUiState.Error -> {
                LaunchedEffect(uiState.message) {
                    // Show error snackbar
                }
            }
            else -> {}
        }
    }
}
```

### 7. Activity Implementation (for Deep Links)

```kotlin
class ResetPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val token = intent.data?.getQueryParameter("token")
        
        setContent {
            YourAppTheme {
                ResetPasswordScreen(
                    token = token,
                    onPasswordResetSuccess = {
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

### 8. Error Handling Best Practices

1. **Token Validation:**
   - Check if token exists when screen loads
   - Show appropriate error if token is missing
   - Handle invalid/expired tokens gracefully

2. **Password Validation:**
   - Minimum 6 characters
   - Real-time validation feedback
   - Password match validation
   - Show clear error messages

3. **Network Errors:**
   - Check for internet connectivity
   - Handle timeout errors
   - Show retry option for network failures

4. **API Errors:**
   - Parse error response body
   - Show user-friendly messages:
     - Invalid token: "This reset link is invalid. Please request a new password reset."
     - Expired token: "This reset link has expired. Please request a new password reset."
     - Generic error: "Something went wrong. Please try again."

### 9. Testing Checklist

- [ ] Test with valid token
- [ ] Test with invalid token (400 error)
- [ ] Test with expired token (400 error)
- [ ] Test password validation (minimum 6 characters)
- [ ] Test password match validation
- [ ] Test network error handling
- [ ] Test deep link with token
- [ ] Test deep link without token
- [ ] Test button disabled states
- [ ] Test success flow and navigation
- [ ] Test show/hide password toggle
- [ ] Test keyboard navigation

### 10. Security Considerations

1. **Token Handling:**
   - Don't log tokens in logs
   - Clear token from memory after use
   - Don't store tokens persistently

2. **Password Handling:**
   - Use secure text input fields
   - Don't log passwords
   - Clear password fields after successful reset

3. **Deep Link Security:**
   - Validate token format before API call
   - Handle malicious or malformed tokens
   - Don't expose tokens in error messages

### 11. Additional Features (Optional)

1. **Password Strength Indicator:**
   - Show visual indicator (weak/medium/strong)
   - Provide feedback on password requirements

2. **Auto-fill Support:**
   - Support Android's Autofill framework
   - Save new password to credential manager

3. **Biometric Authentication:**
   - After password reset, prompt to enable biometric login
   - Link to security settings

4. **Accessibility:**
   - Add content descriptions
   - Ensure proper focus order
   - Support screen readers
   - Support keyboard navigation

## Integration Notes

- This endpoint works universally for both **users** and **vendors** - no need to distinguish between them
- The token is extracted from the email link's query parameter
- Tokens expire after 24 hours
- After successful reset, user should be redirected to login screen
- Consider showing a success message before navigation

## Example API Call

```kotlin
// Using Retrofit with RequestBody
val requestBody = newPassword.toRequestBody("application/json".toMediaType())
val response = apiService.resetPassword(token, requestBody)

// Alternative: Using OkHttp directly
val json = "\"$newPassword\""  // JSON string format
val requestBody = json.toRequestBody("application/json".toMediaType())
val request = Request.Builder()
    .url("http://localhost:8080/auth/reset-password?token=$token")
    .post(requestBody)
    .build()
```

## Custom Retrofit Converter (Optional)

If you need to send plain string as JSON string, create a custom converter:

```kotlin
class StringRequestBodyConverter : Converter<String, RequestBody> {
    override fun convert(value: String): RequestBody {
        // Wrap string in quotes to make it a valid JSON string
        val jsonString = "\"$value\""
        return jsonString.toRequestBody("application/json".toMediaType())
    }
}
```

## Flow Diagram

```
User clicks email link
    ↓
Deep link opens app
    ↓
Extract token from URL
    ↓
Show Reset Password screen
    ↓
User enters new password
    ↓
Validate password (length, match)
    ↓
Call API: POST /auth/reset-password?token={token}
    ↓
Success → Show success message → Navigate to Login
Error → Show error message → Option to request new reset link
```

## Related Implementation

This feature works in conjunction with the Forgot Password feature. See `ANDROID_FORGOT_PASSWORD_IMPLEMENTATION_PROMPT.md` for the complete flow.

