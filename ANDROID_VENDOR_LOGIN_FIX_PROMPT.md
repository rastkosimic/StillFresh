# Android Fix: Vendor Login Fails After Successful Auth

Use this document with your AI agent building the StillFresh Android app. It fixes the post-login failure where **vendor login succeeds on the backend** but the app shows *"Nije moguće učitati profil"* and logs the user out.

---

## Symptom

After email/password login as `VENDOR` or `VENDOR_ADMIN`:

1. `POST /auth/login` returns **200 OK**
2. Vendor API calls (`GET /vendors/onboarding/status`, `GET /vendors/{id}`, dashboard) may succeed
3. App then calls **`POST /auth/refresh`** → **404**
4. App calls **`POST /auth/logout`** and shows profile load error

Customer login may hit the same refresh bug; vendor login also fails if the app loads profile via `GET /users`.

---

## Root causes (Android-side only)

### 1. Wrong refresh endpoint

The backend exposes **`POST /auth/refresh-token`**, not `POST /auth/refresh`.

Calling `/auth/refresh` returns 404 and must not trigger logout during a healthy login session.

### 2. Customer profile endpoint used for vendors

`GET /users` is restricted to role **`USER`** only (`@PreAuthorize("hasRole('USER')")` in user-service).

Vendors must **not** call `GET /users` after login. Use vendor-service endpoints instead.

### 3. Unnecessary refresh immediately after login

After a successful login, the response already contains fresh `accessJwt` and `refreshToken`. Do **not** call refresh before the first authenticated request unless the access JWT is within 5 minutes of expiry (see refresh timing rules below).

---

## Required fixes

### A) Shared auth layer (all roles)

| Item | Correct behavior |
|------|------------------|
| Refresh URL | `POST /auth/refresh-token` |
| Refresh body | `{ "refreshToken": "<stored-refresh-token>" }` |
| Refresh headers | **No** `Authorization` header |
| Token storage | Persist both `accessJwt` and `refreshToken` from login response |
| Bearer header | Use `accessJwt` (fallback to legacy `jwt` only if `accessJwt` is null) |
| After login | Store tokens and proceed — do **not** refresh immediately |
| On refresh failure | Treat **401** from refresh as logged out; **404** means wrong URL — fix the client, do not logout a fresh session |

**Refresh response (200):**

```json
{
  "accessJwt": "<new-access-jwt>",
  "refreshToken": "<new-refresh-token>",
  "jwt": "<new-access-jwt>"
}
```

Replace **both** stored tokens after every successful refresh (rotation).

### B) Post-login flow by role

```kotlin
when (role) {
    "USER" -> {
        // Customer profile from user-service
        GET /users
    }
    "VENDOR", "VENDOR_ADMIN" -> {
        // Do NOT call GET /users
        GET /vendors/onboarding/status   // allowed for both VENDOR and VENDOR_ADMIN
        // Load vendor record (id from onboarding status `id` or JWT userId claim)
        GET /vendors/{vendorId}
    }
}
```

Navigate to the vendor home/onboarding graph only after vendor bootstrap succeeds. Do not reuse the customer `fetchProfile()` path for vendor roles.

**Workers (`role == "VENDOR"` with non-null `assignedLocationId`):**

- `GET /vendors/onboarding/status` returns `status: "COMPLETED"` plus `assignedLocationId`.
- Skip payment / banking / chain / onboarding **write** endpoints (`/vendors/payment/*`, `/vendors/mor/*`, `/vendors/chain/*`, `/vendors/onboarding/set-*`). Those stay `VENDOR_ADMIN`-only and will return Access Denied.
- Offer APIs attribute to the assigned location automatically; do not call them with the worker's own id as a “location”.
- Go straight to the worker home (offers), not the onboarding wizard.

If the app still fires `GET /vendors/payment/status` for every vendor role after login, **stop calling it for `VENDOR`** — that is the second Access Denied you will see after onboarding/status is fixed.

---

## Suggested code changes (where to look)

1. **Retrofit / AuthApi** — ensure refresh method uses `@POST("auth/refresh-token")`, not `auth/refresh`.
2. **OkHttp interceptor / AuthManager** — `ensureValidAccessJwt()` must call the correct endpoint; skip refresh when tokens were just issued at login.
3. **LoginViewModel / SessionManager** — branch post-login bootstrap by `role` from login response.
4. **Error handling** — distinguish vendor bootstrap failure from customer profile failure; avoid generic "Unable to load profile" when the real failure was refresh 404.

---

## Acceptance checklist

- [ ] Vendor email login completes without calling `POST /auth/refresh`.
- [ ] Any refresh call uses `POST /auth/refresh-token` only.
- [ ] After vendor login, app does **not** call `GET /users`.
- [ ] After vendor login, app calls `GET /vendors/onboarding/status` (and vendor profile as needed).
- [ ] Worker (`VENDOR`) login succeeds on `GET /vendors/onboarding/status` (backend allows both roles).
- [ ] Worker login does **not** call payment/banking/chain admin endpoints.
- [ ] No automatic logout when login returned 200 and vendor bootstrap succeeds.
- [ ] Customer (`USER`) login still calls `GET /users` as before.

---

## Related docs in this repo

- `ANDROID_JWT_TOKEN_REFRESH_IMPLEMENTATION_PROMPT.md` — refresh contract and timing rules
- `ANDROID_ACCESS_REFRESH_TOKEN_INTEGRATION_PROMPT.md` — AuthManager / interceptor blueprint
- `ANDROID_USER_PROFILE_INTEGRATION_PROMPT.md` — **customer only** (`GET /users`)
- `ANDROID_VENDOR_ONBOARDING_INTEGRATION_GUIDE.md` — vendor endpoints after login
