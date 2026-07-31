## Auth token model migration (v1 -> access/refresh)

### What changed

- **New**: `accessJwt` (short-lived) + `refreshToken` (long-lived, rotated, Redis-backed).
- **Old (v1)**: a single `jwt` used for everything, including refresh.

### Backward compatibility strategy (implemented)

- **Login response is additive**: `POST /auth/login` now returns:
  - `accessJwt` (new)
  - `refreshToken` (new)
  - `jwt` (legacy, still returned) — **mirrors `accessJwt`**
  - `role`, `accountWasDeleted` (existing fields)

This means:
- Legacy clients that only read `jwt` keep working (they’ll simply behave like “access-token-only” clients).
- Updated clients should switch to `accessJwt` + `refreshToken`.

### Endpoint behavior changes

- **`POST /auth/refresh-token`**
  - Now expects a **refresh token** in the body: `{ "refreshToken": "..." }`
  - Returns JSON `{ "accessJwt": "...", "refreshToken": "...", "jwt": "..." }`
  - Rotation is enabled: the provided refresh token becomes invalid after use.
  - The endpoint is public; clients must not send `Authorization` here.

- **`POST /auth/logout`**
  - Still invalidates the current access token (blacklists its `jti`).
  - If the client includes `{ "refreshToken": "..." }`, that refresh token is revoked too.

### Gateway enforcement

- The API Gateway now **rejects non-access tokens** used as `Authorization: Bearer ...`.
  - This prevents a refresh token from being accepted as a bearer token by mistake.

### Deprecation plan

- **Phase 0 (now)**: ship additive fields, update mobile/web clients.
- **Phase 1 (after clients updated)**:
  - Start logging/metrics on login responses where clients only use `jwt` (if/when client telemetry exists).
  - Add release notes: “`jwt` field deprecated; use `accessJwt`.”
- **Phase 2 (date-based, e.g. +60–90 days)**:
  - Remove `jwt` from responses (or keep it as an alias if you decide it’s harmless).
  - If any legacy client still calls refresh with an access token, it will fail; that client must be updated.

