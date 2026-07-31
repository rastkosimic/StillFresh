# User profile deletion – Android integration prompt

Use this document with your AI agent that is building the StillFresh Android app. It describes how profile deletion works on the backend and gives concrete prompts to implement the flow in the app.

**Note:** For the full flow including **login with previously deleted credentials** (reactivation + `accountWasDeleted` flag) and **both user and vendor** delete/reactivation, use **[ANDROID_DELETE_ACCOUNT_AND_REACTIVATION_PROMPT.md](ANDROID_DELETE_ACCOUNT_AND_REACTIVATION_PROMPT.md)** as the canonical prompt for the Android AI agent.

---

## Backend behaviour (reference)

- **Endpoint:** `DELETE /users/delete`
- **Auth:** Required. Send the current JWT in the `Authorization: Bearer <token>` header (same as other protected user endpoints).
- **Path:** Under the user-service base path (e.g. via API gateway: `DELETE /user-service/users/delete` or as configured in your app).
- **Response on success:** `200 OK` with body `"User profile deleted successfully"` (plain text or similar; confirm exact type from your client).
- **What the backend does:**
  - Identifies the current user from the token (or gateway headers).
  - Optionally accepts a **request body** for deletion feedback: `{ "reason": "other" | "too_expensive" | "not_using" | "privacy", "message": "optional free text" }`. Both fields are optional. If provided, the backend persists this (e.g. for analytics) then performs deletion.
  - Sets the user’s status to **DELETED** (soft delete; row stays, status changes).
  - Removes the user’s favorites.
  - Blacklists the current JWT so it can’t be used again.
- **After a successful call:** The same token must not be used for any further API calls. The app must clear the stored token/session and treat the user as logged out.

**Login with deleted-account credentials:** If a user tries to log in (password or Google OAuth2) with credentials that belong to a deleted account, the backend **allows** login and returns **200 OK** with the usual login response (JWT, role) plus **`accountWasDeleted: true`**. The app should treat as successful login and show a short "Welcome back" message. See **ANDROID_DELETE_ACCOUNT_AND_REACTIVATION_PROMPT.md** for the full contract. (Previously described as: backend returns **410 Gone** with a JSON body that includes `"code": "ACCOUNT_DELETED"` (and `errorMessage`). The app can detect this (e.g. `error.status === 410` or `error.body?.code === 'ACCOUNT_DELETED'`) and show a specific message such as “This account has been deleted. If you’d like to use our service again, please create a new account.” and optionally a button to go to registration.

---

## What the Android app must do

1. **Expose “Delete account” in the UI**  
   e.g. in Profile or Settings, with a clear label and optional confirmation.

2. **Call the API**  
   `DELETE /users/delete` with the current auth token. You may optionally send a JSON body: `{ "reason": "other" | "too_expensive" | "not_using" | "privacy", "message": "optional text" }` (e.g. after asking “Why are you leaving?”).

3. **On success (2xx):**
   - Clear any locally stored tokens (e.g. access + refresh).
   - Clear in-memory user/session state.
   - Navigate to the login/onboarding screen and prevent going back to authenticated screens.

4. **On error (4xx/5xx or network):**
   - Show a user-friendly message (e.g. “Could not delete account. Try again later.”).
   - Do not clear token or session; user remains logged in.

5. **Optional but recommended:**  
   Show a confirmation dialog (“Are you sure you want to delete your account? This cannot be undone.”) before calling the API.

---

## Prompts for the AI agent (Android)

Copy-paste these into your Android AI assistant, in order.

---

### 1. API and repository layer

> **Prompt:**  
> In the StillFresh Android app we need to integrate **user profile deletion**.  
> The backend exposes:  
> - **DELETE /users/delete** with an optional request body: `{ "reason": "other" | "too_expensive" | "not_using" | "privacy", "message": "optional text" }`.  
> - Requires the same auth as other user endpoints (Bearer token in `Authorization` header).  
> - On success returns 200 with a success message; the backend then invalidates the token.  
>  
> Tasks:  
> 1. Add a method in the Retrofit (or equivalent) API interface for `DELETE /users/delete`.  
> 2. In the repository/layer that handles user or auth, add a function (e.g. `deleteAccount()` or `deleteUserProfile()`) that calls this endpoint using the current token.  
> 3. The function should return a result type that allows the UI to distinguish success vs failure (e.g. `Result.success` / `Result.failure`, or sealed class with Success/Error).  
>  
> Do not clear tokens or navigate in the repository; only perform the network call and expose the result.

---

### 2. ViewModel and one-shot event

> **Prompt:**  
> Add support for **delete account** in the ViewModel that backs the profile or settings screen.  
>  
> 1. Expose a method such as `deleteAccount()` or `onDeleteAccountRequested()` that calls the repository’s delete-profile API.  
> 2. Use a one-shot event (e.g. `SharedFlow`, `LiveData`, or `StateFlow` of a sealed class) to signal “delete succeeded” to the UI. The UI will clear tokens, clear session, and navigate to login when it receives this event.  
> 3. Expose loading and error state so the UI can show a progress indicator and an error message (e.g. “Could not delete account. Try again.”) on failure.  
> 4. Optionally expose a “confirm delete” state so the UI can show a confirmation dialog before calling the ViewModel’s delete method.  
>  
> Show the relevant ViewModel code and how the one-shot success event is consumed (e.g. in a Fragment or Composable).

---

### 3. UI: “Delete account” entry and confirmation

> **Prompt:**  
> In the StillFresh Android app, add the **Delete account** entry to the profile or settings screen.  
>  
> 1. Add a row/button labeled like “Delete account” (and optionally a destructive style, e.g. red text).  
> 2. When the user taps it, show a confirmation dialog: title “Delete account?”, message “Are you sure you want to permanently delete your account? This action cannot be undone.” with buttons “Cancel” and “Delete” (or “Delete account”).  
> 3. On “Delete” (confirm), call the ViewModel’s delete-account method. Show a loading indicator (e.g. full-screen or in-dialog) while the request is in progress.  
> 4. On success: clear the stored auth tokens and any in-memory user/session state, then navigate to the login (or onboarding) screen and clear the back stack so the user cannot press Back into authenticated screens.  
> 5. On failure: dismiss loading, show a Snackbar or Toast like “Could not delete account. Please try again.”  
>  
> Implement this in the existing profile/settings UI (XML or Compose) and wire it to the ViewModel and navigation.

---

### 4. Clearing token and session after successful delete

> **Prompt:**  
> When **delete account** succeeds (backend returns 2xx for DELETE /users/delete), the app must:  
> 1. Clear the stored access token (and refresh token if we store it).  
> 2. Clear any in-memory user/session or “logged-in” state.  
> 3. Navigate to the login (or welcome/onboarding) screen.  
> 4. Clear the back stack so the user cannot navigate back to profile or other authenticated screens.  
>  
> Ensure this logic runs in the UI layer when it receives the “delete succeeded” one-shot event from the ViewModel. Show where token storage is cleared and where navigation + back-stack clear is triggered.

---

### 5. Error handling and edge cases

> **Prompt:**  
> For **delete account** in the StillFresh Android app, handle these cases:  
> 1. **Network error or timeout:** Show a friendly message and keep the user on the screen; do not clear token.  
> 2. **401 Unauthorized:** Treat as session invalid; clear token and redirect to login (same as logout).  
> 3. **Other 4xx/5xx:** Show a generic “Could not delete account. Please try again later.” (or use server message if available).  
> 6. **Login with deleted-account credentials:** If the user later tries to log in with the same credentials, the backend returns **200 OK** with `accountWasDeleted: true`; treat as successful login and show "Welcome back". Handle this in the login flow: show “This account has been deleted. If you’d like to use our service again, please create a new account.” and optionally a “Sign up” button.  
> 4. **User taps “Cancel” in the confirmation dialog:** Do nothing; no API call.  
> 5. **Avoid double submit:** Disable the “Delete” button or dialog while the request is in progress.  
>  
> Implement these in the ViewModel and UI that handle delete account.

---

## Acceptance checklist (Android)

- [ ] Profile or settings has a “Delete account” entry (destructive style).
- [ ] Tapping it shows a confirmation dialog; “Cancel” closes without calling the API.
- [ ] On “Delete”/confirm, the app calls `DELETE /users/delete` with the current auth token.
- [ ] Loading state is shown during the request.
- [ ] On success: stored tokens and session are cleared, app navigates to login (or onboarding) with back stack cleared.
- [ ] On failure: error message is shown; user remains logged in and can retry or leave.
- [ ] No further API calls use the same token after a successful delete.

---

## API summary (quick reference)

| Item        | Value                    |
|------------|---------------------------|
| Method     | `DELETE`                  |
| Path       | `/users/delete`           |
| Headers    | `Authorization: Bearer <token>` |
| Body       | Optional: `{ "reason": "other" \| "too_expensive" \| "not_using" \| "privacy", "message": "optional" }` |
| Success    | `200 OK`, message body   |
| After success | Token invalidated; app must clear token and go to login |
