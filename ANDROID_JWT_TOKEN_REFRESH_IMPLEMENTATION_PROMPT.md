# Android Auth Tokens (access + refresh) – Implementation Prompt

## Purpose

Implement a standard, robust auth model in the StillFresh Android app:

- **Access JWT** (short-lived, ~15 minutes) used in `Authorization: Bearer <accessJwt>`.
- **Refresh token** (long-lived, ~30 days) stored securely on-device and used **only** to mint new access tokens.
- Refresh must work even when the access JWT is expired (common after backgrounding the app).
- Refresh tokens are **rotated** by default (one-time use) so you must always replace the stored refresh token after refresh.

This document is written for an AI agent or developer implementing the Android client.

---

## Backend contract (current)

### Login

- **Endpoint:** `POST /auth/login`
- **Success response (JSON):**

```json
{
  "accessJwt": "<access-jwt>",
  "refreshToken": "<refresh-token>",
  "jwt": "<access-jwt>", 
  "role": "USER",
  "accountWasDeleted": false
}
```

Notes:
- `jwt` is **legacy** and currently mirrors `accessJwt`. New code should use `accessJwt`.

### Refresh

- **Endpoint:** `POST /auth/refresh-token`
- **Authentication:** none (do **not** send `Authorization` header).
- **Request body (JSON):**

```json
{
  "refreshToken": "<refresh-token>"
}
```

- **Success response (200 OK, JSON):**

```json
{
  "accessJwt": "<new-access-jwt>",
  "refreshToken": "<new-refresh-token>",
  "jwt": "<new-access-jwt>"
}
```

### Logout

- **Endpoint:** `POST /auth/logout`
- **Authentication:** send `Authorization: Bearer <accessJwt>`
- **Request body:** optionally include the refresh token so the backend can revoke it:

```json
{
  "refreshToken": "<refresh-token>"
}
```

---

## Token lifetimes + exact refresh timing rules

- **Access JWT lifetime:** ~15 minutes
- **Refresh token lifetime:** ~30 days

**Exact proactive refresh rule (before any authenticated request and on app resume):**

- Decode the **access JWT** payload and read `exp` (Unix seconds).
- Let `nowSec = System.currentTimeMillis() / 1000`.
- If `exp - nowSec <= 300` (≤ 5 minutes remaining), refresh **before** sending the API request.

**Exact reactive refresh rule (on 401):**

- If a request fails with `401`, refresh **once**, then retry the original request once with the new access JWT.
- If refresh fails with `401`, clear tokens and route to login.

---

## What to implement

### 1. Token storage (secure, correct, minimal)

- **Store refresh token in** `EncryptedSharedPreferences` (or Keystore-backed secure storage).
- **Store accessJwt in memory** (preferred) and optionally persist it (also encrypted) to survive process death.
- Store these keys:
  - `accessJwt`
  - `refreshToken`
  - `role` (optional convenience; still treat server as source of truth)

Hard rules:
- Never log tokens.
- Never put tokens in analytics/crash breadcrumbs.
- Treat refresh token as the highest-value secret on device.

### 2. Proactive refresh (foreground + before request)

Implement a single function, conceptually:

- `suspend fun ensureValidAccessJwt(): String?`
  - If no refresh token: return null (logged out).
  - If accessJwt is missing OR expires in ≤ 5 minutes: call refresh and persist returned tokens.
  - Return a usable accessJwt.

Call it:
- On app foreground/resume (`ProcessLifecycleOwner` / `onStart` / `onResume` equivalent)
- Immediately before any authenticated request

### 3. Reactive refresh (OkHttp interceptor)

Use an interceptor to handle 401:

- If response is 401:
  - Run refresh **once** (using refresh token only, no Authorization header).
  - If refresh succeeds:
    - Persist new `accessJwt` + `refreshToken`
    - Retry original request with `Authorization: Bearer <newAccessJwt>`
  - If refresh fails:
    - Clear tokens
    - Notify app to navigate to login (e.g. via an auth state flow)

Concurrency requirement:
- Ensure only **one refresh** happens at a time. When multiple requests hit 401 simultaneously, one performs refresh while others await the result and then retry with the new accessJwt.

### 4. Always use accessJwt as Bearer

- Every authenticated API call must send:
  - `Authorization: Bearer <accessJwt>`
- Never send a refresh token in `Authorization` (the gateway rejects non-access tokens as Bearer).

---

## Implementation notes (JWT expiry parsing)

- JWT is `header.payload.signature` (base64url segments).
- Decode **payload** only, parse JSON, read `exp`.
- Use payload parsing only for refresh timing (not for authorization decisions).

---

## Summary checklist

- [ ] Persist `refreshToken` in `EncryptedSharedPreferences` (secure storage).
- [ ] Keep `accessJwt` available for `Authorization: Bearer ...` and refresh it when `exp - now <= 300`.
- [ ] Call refresh on app resume and before requests when within the 5-minute window.
- [ ] On `401`: refresh once, retry request once; on refresh 401 → clear tokens and login.
- [ ] Rotation support: always **replace** stored refresh token with the new one from refresh responses.
- [ ] Never use refresh token as Bearer; never log tokens.
