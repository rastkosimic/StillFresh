# Android Google Sign-In/Sign-Up Implementation Guide

## Overview
This guide explains how to implement Google OAuth2 sign-in/sign-up in the Android mobile app for the StillFresh application. The backend API is already configured and ready to accept Google authentication.

## Backend API Endpoint

**POST** `http://your-api-gateway-url/auth/oauth2/google/login`

**Request Body:**
```json
{
  "idToken": "google-id-token-from-android",
  "role": "USER"  // or "VENDOR" for vendor sign-in
}
```

**Success Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "USER"
}
```

**Error Response (400/500):**
```json
{
  "error": "Error message here"
}
```

## Implementation Steps

### Step 1: Add Google Sign-In Dependencies

Add to your `app/build.gradle` (or `build.gradle.kts`):

```gradle
dependencies {
    // Google Sign-In
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
    
    // HTTP client (if not already included)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
}
```

### Step 2: Configure Google Sign-In in Android

1. **Get SHA-1 Certificate Fingerprint:**
   ```bash
   # For debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # For release keystore
   keytool -list -v -keystore your-release-keystore.jks -alias your-alias
   ```

2. **Add SHA-1 to Google Cloud Console:**
   - Go to Google Cloud Console → APIs & Services → Credentials
   - Edit your OAuth 2.0 Client ID
   - Add Android application with package name and SHA-1 fingerprint
   - Save the changes

3. **Get OAuth 2.0 Client ID for Android:**
   - In Google Cloud Console, create a new OAuth 2.0 Client ID
   - Application type: **Android**
   - Package name: Your app's package name (e.g., `com.stillfresh.app`)
   - SHA-1 certificate fingerprint: From step 1
   - Copy the **Client ID** (looks like: `123456789-abc123.apps.googleusercontent.com`)

### Step 3: Configure AndroidManifest.xml

Add internet permission (if not already present):

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application>
        <!-- Your existing application configuration -->
    </application>
</manifest>
```

### Step 4: Create Google Sign-In Helper Class

Create a new file: `GoogleSignInHelper.kt` (or `.java`)

```kotlin
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class GoogleSignInHelper(private val context: Context) {
    
    private lateinit var googleSignInClient: GoogleSignInClient
    
    // Replace with your Google OAuth Client ID from Google Cloud Console
    private val GOOGLE_CLIENT_ID = "393099083984-tu9ir2350sp8tdeotrb9aeg3pj4r5l5f.apps.googleusercontent.com"
    
    fun initialize() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GOOGLE_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }
    
    fun signIn(activity: Activity, requestCode: Int) {
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, requestCode)
    }
    
    fun signOut() {
        googleSignInClient.signOut()
    }
    
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            account
        } catch (e: ApiException) {
            null
        }
    }
    
    fun getIdToken(account: GoogleSignInAccount, callback: (String?) -> Unit) {
        account.idToken?.let { token ->
            callback(token)
        } ?: run {
            // If token is null, request it again
            account.requestIdToken(GOOGLE_CLIENT_ID)?.let { task ->
                task.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(task.result)
                    } else {
                        callback(null)
                    }
                }
            }
        }
    }
}
```

### Step 5: Create API Service Interface

Create a new file: `AuthApiService.kt`

```kotlin
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class GoogleLoginRequest(
    val idToken: String,
    val role: String
)

data class AuthResponse(
    val token: String,
    val role: String
)

data class ErrorResponse(
    val error: String
)

interface AuthApiService {
    @POST("auth/oauth2/google/login")
    fun googleLogin(@Body request: GoogleLoginRequest): Call<AuthResponse>
}
```

### Step 6: Create Authentication Repository

Create a new file: `AuthRepository.kt`

```kotlin
import android.content.Context
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthRepository(private val context: Context) {
    
    private val BASE_URL = "http://your-api-gateway-url/" // Replace with your actual API Gateway URL
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val authApiService = retrofit.create(AuthApiService::class.java)
    
    fun loginWithGoogle(idToken: String, role: String, callback: (Result<AuthResponse>) -> Unit) {
        val request = GoogleLoginRequest(idToken, role)
        
        authApiService.googleLogin(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    callback(Result.success(response.body()!!))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Login failed: $errorBody")
                    callback(Result.failure(Exception("Login failed: ${response.message()}")))
                }
            }
            
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("AuthRepository", "Network error", t)
                callback(Result.failure(t))
            }
        })
    }
}
```

### Step 7: Implement in Activity/Fragment

Example implementation in your Login/SignUp Activity:

```kotlin
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class LoginActivity : AppCompatActivity() {
    
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var authRepository: AuthRepository
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize Google Sign-In
        googleSignInHelper = GoogleSignInHelper(this)
        googleSignInHelper.initialize()
        
        // Initialize Auth Repository
        authRepository = AuthRepository(this)
        
        // Set up Google Sign-In button click listener
        findViewById<Button>(R.id.btnGoogleSignIn).setOnClickListener {
            signInWithGoogle("USER") // Use "VENDOR" for vendor sign-in
        }
    }
    
    private fun signInWithGoogle(role: String) {
        googleSignInHelper.signIn(this, GOOGLE_SIGN_IN_REQUEST_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val account = googleSignInHelper.handleSignInResult(data)
                
                if (account != null) {
                    // Get ID token from Google account
                    googleSignInHelper.getIdToken(account) { idToken ->
                        if (idToken != null) {
                            // Send ID token to backend
                            authenticateWithBackend(idToken, "USER") // or "VENDOR"
                        } else {
                            Toast.makeText(this, "Failed to get Google ID token", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun authenticateWithBackend(idToken: String, role: String) {
        // Show loading indicator
        showLoading(true)
        
        authRepository.loginWithGoogle(idToken, role) { result ->
            runOnUiThread {
                showLoading(false)
                
                result.onSuccess { authResponse ->
                    // Save JWT token (use SharedPreferences or secure storage)
                    saveAuthToken(authResponse.token)
                    saveUserRole(authResponse.role)
                    
                    // Navigate to main screen
                    navigateToMainScreen()
                    
                    Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show()
                }.onFailure { exception ->
                    Toast.makeText(this, "Sign in failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun saveAuthToken(token: String) {
        val prefs = getSharedPreferences("StillFreshPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("auth_token", token).apply()
    }
    
    private fun saveUserRole(role: String) {
        val prefs = getSharedPreferences("StillFreshPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_role", role).apply()
    }
    
    private fun navigateToMainScreen() {
        // Navigate to your main activity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun showLoading(show: Boolean) {
        // Show/hide loading indicator
        findViewById<ProgressBar>(R.id.progressBar).visibility = 
            if (show) View.VISIBLE else View.GONE
    }
}
```

### Step 8: Use JWT Token for Authenticated Requests

After successful login, include the JWT token in all authenticated API requests:

```kotlin
// Add interceptor to OkHttpClient
val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${getAuthToken()}")
            .build()
        chain.proceed(request)
    }
    .build()

private fun getAuthToken(): String? {
    val prefs = getSharedPreferences("StillFreshPrefs", Context.MODE_PRIVATE)
    return prefs.getString("auth_token", null)
}
```

## User Flow

1. **User taps "Sign in with Google" button**
2. **Google Sign-In dialog appears** (handled by Google SDK)
3. **User selects Google account and grants permissions**
4. **App receives Google ID token**
5. **App sends ID token to backend** via `POST /auth/oauth2/google/login`
6. **Backend validates token, creates/updates user, returns JWT**
7. **App saves JWT token** for future authenticated requests
8. **User is signed in** and navigated to main screen

## Error Handling

Handle these scenarios:

1. **Google Sign-In Cancelled**: User closes the Google sign-in dialog
2. **Network Error**: No internet connection or API unavailable
3. **Invalid Token**: Google ID token is invalid or expired
4. **Backend Error**: Server returns error (400/500)

## Testing Checklist

- [ ] Google Sign-In button appears and is clickable
- [ ] Google Sign-In dialog opens when button is clicked
- [ ] User can select Google account
- [ ] ID token is successfully retrieved from Google
- [ ] ID token is sent to backend API
- [ ] JWT token is received and saved
- [ ] User is navigated to main screen after successful login
- [ ] JWT token is included in subsequent API requests
- [ ] Error messages are displayed appropriately
- [ ] Works for both USER and VENDOR roles

## Security Best Practices

1. **Store JWT Securely**: Use Android Keystore or encrypted SharedPreferences
2. **Validate Token Expiry**: Check token expiration before making requests
3. **Handle Token Refresh**: Implement token refresh logic if needed
4. **Use HTTPS**: Always use HTTPS for API calls in production
5. **Validate Response**: Always validate API responses before using data

## Troubleshooting

### Issue: "DEVELOPER_ERROR" when signing in
- **Solution**: Verify SHA-1 fingerprint is correctly added in Google Cloud Console

### Issue: "10:" error code
- **Solution**: Check that Google Sign-In API is enabled in Google Cloud Console

### Issue: ID token is null
- **Solution**: Ensure `requestIdToken()` is called with correct Client ID

### Issue: Backend returns 400/500 error
- **Solution**: Verify API Gateway URL is correct and backend is running

## Additional Resources

- [Google Sign-In for Android Documentation](https://developers.google.com/identity/sign-in/android)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)

## Notes

- Replace `your-api-gateway-url` with your actual API Gateway URL (e.g., `http://localhost:8080` for development)
- The Google Client ID in the code should match the Android OAuth 2.0 Client ID from Google Cloud Console
- For production, use environment variables or build configs for sensitive values
- Consider implementing token refresh mechanism for long-lived sessions

