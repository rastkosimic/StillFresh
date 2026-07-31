# Delete account and login reactivation – Android integration prompt

Use this document with your AI agent that is building the StillFresh Android app. It describes how **account deletion** and **login with previously deleted credentials** work on the backend for **both users and vendors**, and gives concrete prompts to implement the flows in the app.

---

## Backend behaviour (summary)

- **Delete account (user or vendor):** The backend soft-deletes the account (status DELETED), invalidates the token, and optionally stores deletion feedback (reason + message). The app must clear tokens and navigate to login on success.
- **Login with deleted-account credentials:** The backend **allows** the login: it reactivates the account (sets status ACTIVE), returns **200 OK** with the usual login response (JWT, role) **plus** a flag **`accountWasDeleted: true`**. The app must treat this as a successful login and can show a “Welcome back, your account was previously deleted” message (or similar).

The same logic applies to **users** (customer app) and **vendors** (vendor app / vendor role).

---

## Part 1 – User account

### 1.1 Delete user account

| Item | Value |
|------|--------|
| Method | `DELETE` |
| Path | `/users/delete` (under user-service base path, e.g. via gateway: `DELETE /user-service/users/delete`) |
| Auth | Required. `Authorization: Bearer <token>` |
| Body | Optional JSON: `{ "reason": "other" \| "too_expensive" \| "not_using" \| "privacy", "message": "optional free text" }` |
| Success | `200 OK` with body e.g. `"User profile deleted successfully"` (string) |
| After success | Token is invalidated. App must clear stored tokens and session and navigate to login. |

**What the backend does:** Identifies the user from the token, optionally persists reason/message, sets user status to DELETED, removes favorites, blacklists the JWT.

### 1.2 Login response (auth) – when account was previously deleted

Login endpoints: **POST /auth/login** (identifier + password) and **POST /auth/oauth2/google/login** (Google ID token). Same response shape for both.

When the credentials belong to a **deleted** account, the backend **does not** return 410. It **reactivates** the account and returns **200 OK** with the normal login payload plus a flag:

**Success response (200 OK) – JSON body example:**

```json
{
  "jwt": "<access token>",
  "role": "USER",
  "accountWasDeleted": true
}
```

- **`accountWasDeleted`** may be `true`, `false`, or omitted (treat omitted as `false`).
- When **`accountWasDeleted === true`**, the account had been deleted and was just reactivated. The user is logged in; the app should show a short “Welcome back – your account was previously deleted” (or similar) message and then continue to the main app.

---

## Part 2 – Vendor account

### 2.1 Delete vendor account

| Item | Value |
|------|--------|
| Method | `DELETE` |
| Path | `/vendors/delete` (under vendor-service base path, e.g. via gateway: `DELETE /vendor-service/vendors/delete`) |
| Auth | Required. `Authorization: Bearer <token>` (VENDOR_ADMIN) |
| Body | Optional JSON: `{ "reason": "other" \| "too_expensive" \| "not_using" \| "privacy", "message": "optional free text" }` |
| Success | `200 OK` with body e.g. `"Vendor deleted successfully"` (string) |
| After success | Token is invalidated. App must clear stored tokens and session and navigate to login. |

**What the backend does:** Identifies the vendor from the token, optionally persists reason/message, sets vendor status to DELETED, publishes events, invalidates the JWT.

### 2.2 Login response (auth) – when vendor account was previously deleted

Vendors use the **same auth login endpoints** (POST /auth/login or POST /auth/oauth2/google/login) with role VENDOR or VENDOR_ADMIN. When the credentials belong to a **deleted** vendor account, the backend reactivates and returns **200 OK** with:

**Success response (200 OK) – JSON body example:**

```json
{
  "jwt": "<access token>",
  "role": "VENDOR_ADMIN",
  "accountWasDeleted": true
}
```

Same as user: **`accountWasDeleted: true`** means the vendor account had been deleted and was just reactivated. Treat as successful login and show a “Welcome back” message.

---

## What the Android app must do (both users and vendors)

### Delete account (user or vendor)

1. Expose “Delete account” in Profile/Settings (with confirmation).
2. Optionally ask “Why are you leaving?” and collect **reason** (e.g. other, too_expensive, not_using, privacy) and **message** (free text).
3. Call the correct delete endpoint:
   - **User:** `DELETE /users/delete` with optional body `{ "reason", "message" }`.
   - **Vendor:** `DELETE /vendors/delete` with optional body `{ "reason", "message" }`.
4. On **success (2xx):** Clear tokens and session, navigate to login, clear back stack.
5. On **error:** Show a friendly error; do not clear token.

### Login (user or vendor)

1. On **successful login (200 OK):** Parse the response body (e.g. `jwt`, `role`, `accountWasDeleted`).
2. If **`accountWasDeleted === true`** (or `accountWasDeleted == true` in Kotlin):
   - Treat as **logged in** (store JWT, navigate to main app).
   - Show a one-time message such as: “Welcome back. Your account had been deleted and has been reactivated.” (or “Nice to have you back.”). Then continue normally.
3. If **`accountWasDeleted`** is false or absent: proceed as usual (no special message).

---

## Prompts for the Android AI agent

Copy-paste these into your Android AI assistant. Implement for **both** user and vendor flows where applicable.

---

### Prompt 1 – API and repository (delete account)

> **Prompt:**  
> In the StillFresh Android app we need **delete account** for both **users** and **vendors**.  
>  
> **User:**  
> - **DELETE /users/delete** (user-service path). Auth: Bearer token.  
> - Optional body: `{ "reason": "other" | "too_expensive" | "not_using" | "privacy", "message": "optional string" }`.  
> - Success: 200 with string body (e.g. "User profile deleted successfully").  
>  
> **Vendor:**  
> - **DELETE /vendors/delete** (vendor-service path). Auth: Bearer token.  
> - Optional body: same shape as user.  
> - Success: 200 with string body (e.g. "Vendor deleted successfully").  
>  
> Tasks:  
> 1. Add Retrofit (or equivalent) methods for both endpoints, with optional request body for reason/message.  
> 2. In the repository layer, add functions e.g. `deleteUserAccount(reason?, message?)` and `deleteVendorAccount(reason?, message?)` that call the correct endpoint with the current token.  
> 3. Return a result type (e.g. `Result.success` / `Result.failure` or sealed class) so the UI can distinguish success vs failure.  
> Do not clear tokens or navigate in the repository; only perform the network call and expose the result.

---

### Prompt 2 – Login response model and accountWasDeleted

> **Prompt:**  
> The StillFresh backend login response (POST /auth/login and POST /auth/oauth2/google/login) includes an optional field **accountWasDeleted** (Boolean).  
>  
> Example JSON:  
> `{ "jwt": "...", "role": "USER", "accountWasDeleted": true }`  
>  
> Tasks:  
> 1. Ensure the login response DTO/model includes **accountWasDeleted** (nullable Boolean or optional).  
> 2. When parsing a successful login response (200 OK), read **accountWasDeleted**. If it is `true`, the account was previously deleted and has been reactivated; the user/vendor is logged in and the app should show a short “Welcome back – your account was previously deleted” (or “Nice to have you back”) message, then continue to the main screen.  
> 3. Do not treat **accountWasDeleted: true** as an error; it is a successful login with an informational flag.

---

### Prompt 3 – Login flow: handle accountWasDeleted in UI

> **Prompt:**  
> In the StillFresh Android app, after a **successful** login (200 OK with JWT):  
> 1. If the response has **accountWasDeleted == true**, show a short message (e.g. Snackbar, dialog, or inline) such as: “Welcome back. Your account had been deleted and has been reactivated.” or “Nice to have you back.”  
> 2. Then store the JWT, set the user/vendor as logged in, and navigate to the main app (same as a normal login).  
> 3. This applies to both **password login** and **Google OAuth2 login**, and to both **user** (customer) and **vendor** roles.  
> Implement this in the login success handler (e.g. in ViewModel or auth service callback) and ensure the “Welcome back” message is shown only when **accountWasDeleted** is true.

---

### Prompt 4 – Delete account UI and flow (user)

> **Prompt:**  
> In the StillFresh Android app, implement **delete user account** in the user (customer) profile or settings.  
> 1. Add a “Delete account” entry (destructive style).  
> 2. Optionally, before or after a final confirmation, ask “Why are you leaving?” and allow selecting a reason (other, too_expensive, not_using, privacy) and optionally a short message.  
> 3. On confirm: call **DELETE /users/delete** with the current auth token and optional body `{ "reason", "message" }`. Show loading during the request.  
> 4. On success: clear stored tokens and session, navigate to login (or onboarding), clear back stack.  
> 5. On failure: show “Could not delete account. Please try again.” and do not clear token.  
> 6. Show a final confirmation dialog (e.g. “Are you sure you want to permanently delete your account?”) before calling the API.

---

### Prompt 5 – Delete account UI and flow (vendor)

> **Prompt:**  
> In the StillFresh Android app, implement **delete vendor account** in the vendor profile or settings.  
> 1. Add a “Delete account” (or “Delete business account”) entry (destructive style).  
> 2. Optionally ask “Why are you leaving?” with reason (other, too_expensive, not_using, privacy) and optional message.  
> 3. On confirm: call **DELETE /vendors/delete** with the current auth token and optional body `{ "reason", "message" }`. Show loading.  
> 4. On success: clear stored tokens and session, navigate to vendor login (or onboarding), clear back stack.  
> 5. On failure: show a friendly error and do not clear token.  
> 6. Show a final confirmation dialog before calling the API.  
> The behaviour is the same as for user delete, except the endpoint is vendor-service’s **DELETE /vendors/delete**.

---

### Prompt 6 – Clearing token and session after successful delete

> **Prompt:**  
> When **delete account** succeeds (2xx for DELETE /users/delete or DELETE /vendors/delete), the app must:  
> 1. Clear the stored access token (and refresh token if stored).  
> 2. Clear any in-memory user/vendor session state.  
> 3. Navigate to the login (or welcome/onboarding) screen.  
> 4. Clear the back stack so the user cannot go back to authenticated screens.  
> Apply this for both user and vendor delete success handlers.

---

### Prompt 7 – Error handling (delete account)

> **Prompt:**  
> For **delete account** (user and vendor) in the StillFresh Android app, handle:  
> 1. **Network error or timeout:** Show a friendly message; do not clear token.  
> 2. **401 Unauthorized:** Treat as session invalid; clear token and redirect to login.  
> 3. **Other 4xx/5xx:** Show “Could not delete account. Please try again later.” (or server message if available).  
> 4. **User taps “Cancel”:** No API call.  
> 5. **Avoid double submit:** Disable the delete button or dialog while the request is in progress.

---

## Acceptance checklist (Android)

**Delete account (user and vendor)**  
- [ ] User profile/settings has “Delete account” with optional reason/message and confirmation.  
- [ ] Vendor profile/settings has “Delete account” with optional reason/message and confirmation.  
- [ ] On confirm, the app calls the correct delete endpoint (users/delete or vendors/delete) with optional body.  
- [ ] On success: tokens and session cleared, navigate to login, back stack cleared.  
- [ ] On failure: error shown; user/vendor remains logged in.

**Login and accountWasDeleted**  
- [ ] Login response model includes **accountWasDeleted**.  
- [ ] When login returns 200 and **accountWasDeleted == true**, the app shows a “Welcome back” (or similar) message and then continues as logged in (store JWT, go to main app).  
- [ ] This is implemented for both password and Google OAuth2 login, and for both user and vendor.

---

## API quick reference

| Flow | Method | Path | Body (optional) | Success |
|------|--------|------|------------------|--------|
| Delete user | DELETE | /users/delete | `{ "reason", "message" }` | 200, then clear token |
| Delete vendor | DELETE | /vendors/delete | `{ "reason", "message" }` | 200, then clear token |
| Login (user or vendor) | POST | /auth/login or /auth/oauth2/google/login | (login payload) | 200, `{ "jwt", "role", "accountWasDeleted"? }` |

**Login:** If **accountWasDeleted** is true, treat as success and show “Welcome back” message; user/vendor is logged in.
