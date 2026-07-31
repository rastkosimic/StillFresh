# Android Forgot Password Implementation Prompt

## Overview
Implement the "Forgot Password" feature in the Android mobile application. This feature allows users (both regular users and vendors) to request a password reset link via email when they forget their password.

## API Endpoint Details

### Endpoint
```
POST http://localhost:8080/auth/forgot-password?email={email}
```

**Note:** Replace `localhost:8080` with your actual API gateway URL in production.

### Request Details
- **Method:** POST
- **Content-Type:** application/json
- **Authentication:** Not required (public endpoint)
- **Query Parameter:**
  - `email` (String, required): The email address of the user/vendor requesting password reset

### Request Body
No request body required. The email is passed as a query parameter.

### Response Details

**Success Response (200 OK):**
```
"Password reset link sent to your email"
```

**Error Responses:**
- **404 NOT FOUND:** `"User not found with email: {email}"`
- **500 INTERNAL SERVER ERROR:** `"Failed to send password reset link"`

## Implementation Requirements

### 1. UI/UX Design

Create a "Forgot Password" screen with the following elements:

- **Email Input Field:**
  - Use Material Design TextInputLayout with TextInputEditText
  - Add email validation (check for valid email format)
  - Show error messages below the input field
  - Placeholder: "Enter your email address"
  - Input type: `InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS`

- **Submit Button:**
  - Material Design button
  - Text: "Send Reset Link" or "Request Password Reset"
  - Disable button while request is in progress
  - Show loading indicator during API call

- **Back to Login Link:**
  - Text: "Back to Login" or "Remember your password?"
  - Navigate back to login screen

- **Success Message:**
  - Display a dialog or snackbar after successful submission
  - Message: "Password reset link has been sent to your email. Please check your inbox."
  - Optionally show the email address where the link was sent

- **Error Handling:**
  - Display error messages in a user-friendly way
  - Use Snackbar or AlertDialog for error notifications
  - Handle network errors gracefully

### 2. Network Implementation

**Create a Retrofit Interface:**
```kotlin
interface AuthApiService {
    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Query("email") email: String
    ): Response<String>
}
```

**Create a Repository:**
```kotlin
class AuthRepository(private val apiService: AuthApiService) {
    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = apiService.forgotPassword(email)
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
}
```

### 3. ViewModel Implementation

```kotlin
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    
    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.Loading
            
            // Validate email format
            if (!isValidEmail(email)) {
                _uiState.value = ForgotPasswordUiState.Error("Please enter a valid email address")
                return@launch
            }
            
            authRepository.forgotPassword(email)
                .onSuccess { message ->
                    _uiState.value = ForgotPasswordUiState.Success(message)
                }
                .onFailure { exception ->
                    _uiState.value = ForgotPasswordUiState.Error(
                        exception.message ?: "Failed to send password reset link"
                    )
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
sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    data class Success(val message: String) : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}
```

### 5. Compose UI Implementation

```kotlin
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = viewModel(),
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enter your email address and we'll send you a link to reset your password.",
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
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            enabled = uiState !is ForgotPasswordUiState.Loading
        )
        
        Button(
            onClick = { viewModel.requestPasswordReset(email) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = uiState !is ForgotPasswordUiState.Loading && email.isNotBlank()
        ) {
            if (uiState is ForgotPasswordUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send Reset Link")
            }
        }
        
        TextButton(onClick = onBackToLogin) {
            Text("Back to Login")
        }
        
        // Handle UI state
        when (uiState) {
            is ForgotPasswordUiState.Success -> {
                LaunchedEffect(Unit) {
                    // Show success dialog or snackbar
                }
            }
            is ForgotPasswordUiState.Error -> {
                LaunchedEffect(uiState.message) {
                    // Show error snackbar
                }
            }
            else -> {}
        }
    }
}
```

### 6. Error Handling Best Practices

1. **Network Errors:**
   - Check for internet connectivity before making the request
   - Show appropriate message: "Please check your internet connection"
   - Handle timeout errors gracefully

2. **API Errors:**
   - Parse error response body if available
   - Show user-friendly error messages
   - For 404: "No account found with this email address"
   - For 500: "Something went wrong. Please try again later"

3. **Validation Errors:**
   - Validate email format before API call
   - Show inline validation errors
   - Prevent API call if validation fails

### 7. Testing Checklist

- [ ] Test with valid email address
- [ ] Test with invalid email format
- [ ] Test with non-existent email (404 error)
- [ ] Test network error handling (no internet)
- [ ] Test timeout scenarios
- [ ] Test button disabled state during loading
- [ ] Test navigation back to login screen
- [ ] Test success message display
- [ ] Test error message display
- [ ] Test email input validation

### 8. Security Considerations

1. **Rate Limiting:**
   - Consider implementing client-side rate limiting to prevent abuse
   - Show appropriate message if user tries too many times

2. **Email Privacy:**
   - Don't log or store email addresses unnecessarily
   - Clear email input after successful submission (optional)

3. **User Feedback:**
   - Always show the same success message regardless of whether email exists (security best practice)
   - Don't reveal if an email is registered or not

### 9. Additional Features (Optional)

1. **Resend Functionality:**
   - Add a "Resend Email" button that appears after successful submission
   - Implement a cooldown period (e.g., 60 seconds) before allowing resend

2. **Email Auto-fill:**
   - If user is logged in, pre-fill email from user profile
   - Support Android's Autofill framework

3. **Accessibility:**
   - Add content descriptions for screen readers
   - Ensure proper focus order
   - Support keyboard navigation

## Integration Notes

- This endpoint works universally for both **users** and **vendors** - no need to distinguish between them
- The email address is the only identifier needed
- After successful submission, the user will receive an email with a password reset link
- The reset link will contain a token that expires in 24 hours
- Direct users to check their spam folder if they don't receive the email

## Example API Call

```kotlin
// Using Retrofit
val response = apiService.forgotPassword("user@example.com")

// Using OkHttp directly
val request = Request.Builder()
    .url("http://localhost:8080/auth/forgot-password?email=user@example.com")
    .post(RequestBody.create("application/json".toMediaType(), ""))
    .build()
```

## Next Steps

After implementing forgot password, implement the reset password screen that handles the token from the email link. See `ANDROID_RESET_PASSWORD_IMPLEMENTATION_PROMPT.md` for details.

