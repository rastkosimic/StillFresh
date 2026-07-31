# StillFresh Android Integration Prompt: Access JWT + Refresh Token (Rotation)

## Goal

Update the StillFresh Android app to use a robust auth model:

- **`accessJwt`**: short-lived (~15 minutes). Used for all authenticated API calls via `Authorization: Bearer <accessJwt>`.
- **`refreshToken`**: long-lived (~30 days). Stored securely on device and used **only** to mint new access tokens.
- **Rotation enabled**: every successful refresh returns a **new refresh token** and invalidates the old one (one-time use).
- The API Gateway **rejects non-access tokens** in `Authorization: Bearer ...`. Never send refresh tokens as Bearer.

You are an AI Android coding agent. Implement the full flow and update any affected networking/auth code.

---

## Backend API Contract (authoritative)

### 1) Login

**Endpoint:** `POST /auth/login`

**Response (200, JSON):**

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
- `jwt` is **legacy** and currently mirrors `accessJwt`. New client code must use `accessJwt` and `refreshToken`.

### 2) Refresh

**Endpoint:** `POST /auth/refresh-token`

**Authentication:** none (do **not** send `Authorization` header)

**Request (JSON):**

```json
{
  "refreshToken": "<stored-refresh-token>"
}
```

**Response (200, JSON):**

```json
{
  "accessJwt": "<new-access-jwt>",
  "refreshToken": "<new-refresh-token>",
  "jwt": "<new-access-jwt>"
}
```

Failure:
- **401 Unauthorized**: treat as logged out (clear tokens, navigate to login).

### 3) Logout

**Endpoint:** `POST /auth/logout`

**Authentication:** `Authorization: Bearer <accessJwt>`

**Body (optional, JSON):**

```json
{
  "refreshToken": "<stored-refresh-token>"
}
```

Client must still clear tokens locally.

---

## Required Android behavior (do not deviate)

### A) Token storage rules

- **Store `refreshToken` in EncryptedSharedPreferences** (or equivalent Keystore-backed secure storage).
- Store `accessJwt` in memory for normal operation; you may also persist it in EncryptedSharedPreferences to survive process death.
- Never log tokens, never send to analytics, never include in crash breadcrumbs.

Recommended keys:
- `auth.accessJwt`
- `auth.refreshToken`
- `auth.role` (optional convenience)

### B) Bearer header rule (gateway enforcement)

For *all* authenticated API calls:

- `Authorization: Bearer <accessJwt>`

Hard prohibition:
- Never use `refreshToken` as Bearer.
- Never call refresh with an Authorization header.

### C) Refresh timing rules (exact)

**Proactive refresh (before requests + on resume):**

- Decode the **accessJwt** payload and read `exp` (Unix seconds).
- Let `nowSec = System.currentTimeMillis() / 1000`.
- If `accessJwt` is missing OR `exp - nowSec <= 300` (≤ 5 minutes remaining), call refresh **before** sending the API request.

**Reactive refresh (on 401):**

- If a request returns 401:
  - Perform refresh **once**, then retry the original request **once** with the new accessJwt.
  - If refresh fails with 401 → clear tokens and redirect to login.

### D) Rotation rule

On every successful refresh:

- Replace stored `accessJwt` with response `accessJwt`
- Replace stored `refreshToken` with response `refreshToken`

Never keep using the old refresh token after a successful refresh.

---

## Implementation Blueprint (recommended architecture)

Implement a single “AuthStore” + “AuthManager” used by networking:

### 1) `AuthStore`

Responsibilities:
- Get/set `accessJwt`, `refreshToken`
- Clear tokens
- Expose an observable auth state (logged in vs logged out)

### 2) `AuthApi`

Provide suspend functions for:
- `login(...) -> LoginResponse`
- `refreshToken(refreshToken: String) -> TokensResponse`
- `logout(accessJwt: String, refreshToken: String?)`

### 3) `AuthManager`

Provide:
- `suspend fun ensureValidAccessJwt(reason: String): String?`
  - Returns a valid `accessJwt` or `null` if logged out.
  - Performs proactive refresh when needed.

Concurrency requirement:
- Ensure only **one** refresh runs at a time (“single-flight”).
  - If multiple requests trigger refresh concurrently, they must await the same refresh result.
  - Use a `Mutex` (coroutines) or synchronized block + shared `Deferred`.

### 4) OkHttp Interceptor (or Retrofit/OkHttp equivalent)

Two layers are typical:

1) **Request interceptor**:
   - For authenticated requests, call `ensureValidAccessJwt("before_request")`
   - If null: short-circuit as unauthenticated (or proceed without header depending on endpoint)
   - Add `Authorization: Bearer <accessJwt>`

2) **Authenticator / 401 handler**:
   - If response is 401:
     - Call refresh once (single-flight)
     - If refresh succeeded, retry request once with new accessJwt
     - If refresh failed, clear tokens and return original 401

Important:
- Avoid infinite loops: never try to refresh if the failing request is `/auth/refresh-token` or `/auth/login`.

---

## JWT `exp` parsing (Android)

JWT format is `header.payload.signature` (base64url segments).

To read `exp`:
- Split by `.`
- Base64url-decode **payload**
- Parse JSON
- Read `exp` as `Long` (seconds since epoch)

Use the payload only for **timing** decisions, not for authorization/identity.

---

## Test Plan (must pass)

### Login / storage
- Login stores both `accessJwt` and `refreshToken`.
- Bearer header uses only `accessJwt`.

### Proactive refresh
- Force time so `accessJwt` is within 5 minutes of exp → app refreshes before API call.

### Reactive refresh
- Simulate 401 from an authenticated endpoint:
  - App refreshes once and retries original request once.

### Rotation correctness
- After refresh, app uses the **new** refresh token for future refresh calls.
- If you intentionally try to reuse the old refresh token, refresh fails (expected).

### Background resume
- Background app > 15 min, resume, first request triggers refresh and succeeds without forcing re-login.

### Logout
- Logout clears tokens locally.
- If logout sends refresh token in body, server revokes it; subsequent refresh should fail.

---

## Common failure modes (avoid)

- Using `refreshToken` as Bearer → gateway returns 401 (“Invalid token type”).
- Not updating stored `refreshToken` after refresh → next refresh fails (rotation).
- Running multiple refresh calls concurrently → race; one succeeds, others consume/overwrite incorrectly.
- Retrying 401 loops → ensure you refresh only once and retry only once.

