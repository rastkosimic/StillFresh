# Customer User Profile – Android Integration Prompt

Use this document with your AI agent building the StillFresh Android app. It describes how to fetch, display, and edit the logged-in **customer** profile from `user-service`, including behavior after **email/password** and **Google** sign-up.

---

## Context

StillFresh stores customer profile data in **user-service**. The app should treat the backend as the **source of truth** — always load profile via `GET /users` after login and when opening the Profile screen.

All requests go through the **API Gateway** with the same JWT bearer token used for other authenticated calls.

Base path for user endpoints: `/users` (gateway routes to user-service).

---

## Backend `User` response (`GET /users`)

**Endpoint:** `GET /users`  
**Auth:** Bearer token, role `USER`  
**Returns:** JSON `User` object for the authenticated customer.

### Fields returned by the API

| JSON field | Type | Show in Profile UI? | Editable? | Notes |
|---|---|---|---|---|
| `id` | Long | **No** | No | Internal ID; store locally if needed, do not display |
| `username` | String | **Yes** | Limited | Display as handle; backend can update via legacy flows — prefer showing read-only unless you already support username edit |
| `email` | String | **Yes** | Read-only | Account identifier |
| `password` | String | **Never** | No | Hash may appear in JSON — **ignore and never display or persist** |
| `role` | String | **No** | No | Always `USER` for customers |
| `status` | String | **No** | No | Internal (`ACTIVE`, `INACTIVE`, `DELETED`) |
| `firstName` | String? | **Yes** | Yes | Optional |
| `lastName` | String? | **Yes** | Yes | Optional |
| `phoneNumber` | String? | **Yes** | Yes | Optional |
| `address` | String? | **Yes** | Yes | Optional |
| `country` | String? | **Yes** | Yes | Optional; may be a 2-letter code (e.g. `HR`, `US`) when set from Google locale |
| `birthday` | String? | **Yes** | Yes | ISO date `yyyy-MM-dd` when set |
| `dietaryPreference` | String? | **Yes** | Yes | Optional free text |

### Example response

```json
{
  "id": 10042,
  "username": "janedoe",
  "email": "jane@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "firstName": "Jane",
  "lastName": "Doe",
  "phoneNumber": "+385911234567",
  "address": "Ilica 1, Zagreb",
  "country": "HR",
  "birthday": "1990-06-08",
  "dietaryPreference": "Vegetarian"
}
```

---

## What to show on the Profile screen

### Profile section (identity + contact + preferences)

Display these rows, using **"Not set"** (or equivalent) when a nullable field is null/blank:

1. **Display name** — `firstName` + `lastName` (fallback: `username`)
2. **Username** — `@username` (optional second line under name)
3. **Email** — read-only
4. **Phone**
5. **Address**
6. **Country**
7. **Birthday** — format for display (e.g. `8 Jun 1990`), store/send as `yyyy-MM-dd`
8. **Dietary preference**

Suggested header:

```
[Avatar placeholder]
Jane Doe
@janedoe
```

### Shortcuts (separate from profile body)

- **Favorites** → navigate to Favorites screen (`GET /users/favorites`)
- **Settings** → password change, delete account, logout

### Do NOT put on Profile

- Password / change password (Settings)
- Delete account (Settings)
- Internal fields: `id`, `role`, `status`, `password`

---

## Update endpoints

All require bearer token, role `USER`. Each returns the **updated full `User` JSON** — replace local profile state with the response.

| Action | Method | Path | Request body |
|---|---|---|---|
| Update name | `PUT` | `/users/profile/name` | `{ "firstName": "...", "lastName": "..." }` |
| Update phone | `PUT` | `/users/profile/phone` | `{ "phoneNumber": "..." }` |
| Update address | `PUT` | `/users/profile/address` | `{ "address": "..." }` |
| Update country | `PUT` | `/users/profile/country` | `{ "country": "..." }` |
| Update birthday | `PUT` | `/users/profile/birthday` | `{ "birthday": "yyyy-MM-dd" }` |
| Update dietary preference | `PUT` | `/users/profile/dietary-preference` | `{ "dietaryPreference": "..." }` |
| Partial update (any optional fields) | `PUT` | `/users` | Subset of profile fields |

Prefer the **per-field endpoints** for edit flows (clearer intent, matches backend design).

All profile fields are **optional** — empty strings / null are allowed on save.

---

## Google sign-up / sign-in integration

Google login endpoint (via gateway):

**`POST /auth/oauth2/google/login`**

```json
{
  "idToken": "<google-id-token>",
  "role": "USER"
}
```

**Success response:**

```json
{
  "accessJwt": "...",
  "refreshToken": "...",
  "jwt": "...",
  "role": "USER",
  "accountWasDeleted": false
}
```

Use `accessJwt` (or legacy `jwt`) for subsequent API calls.

### What Google provides vs what the backend stores

The **backend** maps Google token claims into user-service profile fields:

| Google claim | Mapped to |
|---|---|
| `given_name` | `firstName` |
| `family_name` | `lastName` |
| `name` (fallback) | Split into `firstName` / `lastName` |
| `locale` (e.g. `en-US`, `hr-HR`) | `country` (region code, e.g. `US`, `HR`) |
| `email` | `email` (also used for username generation) |

Google does **not** populate: `phoneNumber`, `address`, `birthday`, `dietaryPreference`.

### Required app flow after Google login

1. Call `POST /auth/oauth2/google/login` → store tokens.
2. **Immediately call `GET /users`** to load the profile the backend created/merged.
   - Do **not** rely only on `GoogleSignInAccount` local data — the backend is authoritative.
   - Note: user-service creation runs **asynchronously** after first Google sign-up; if `GET /users` fails or returns minimal data on the first attempt, **retry once after a short delay** (e.g. 500 ms – 1 s).
3. Navigate to home; optionally show a **profile completion** prompt if key fields are still empty (`phoneNumber`, `address`, `dietaryPreference`).

If `accountWasDeleted == true`, show a “Welcome back, your account was reactivated” message (see `ANDROID_DELETE_ACCOUNT_AND_REACTIVATION_PROMPT.md`).

### Email/password login

Same rule: after any successful login (email or Google), call **`GET /users`** and cache the result in your session/profile store.

---

## Related endpoints (not part of Profile body)

| Endpoint | Purpose |
|---|---|
| `GET /users/favorites` | Favorited offers with summary counts |
| `GET /users/favorites/count` | `{ "count": N }` for badge/shortcut |
| `PUT /users/change-password` | Settings — see `ANDROID_AUTHENTICATED_PASSWORD_RESET_PROMPT.md` |
| `DELETE /users/delete` | Settings — see `ANDROID_USER_PROFILE_DELETION_INTEGRATION_PROMPT.md` |

---

## High-level implementation goals

1. Extend local `User` / `UserProfile` model with all nullable profile fields (+ ignore `password` on deserialize).
2. Add Retrofit methods for `GET /users` and each `PUT /users/profile/...` endpoint.
3. Repository layer: fetch profile, update each field, persist returned `User` to DataStore/Room/in-memory session.
4. ViewModel: load on profile open; expose edit actions with loading/error state.
5. UI: Profile screen + per-field edit sheets; Settings for password/delete.
6. Login flow: fetch profile after **every** successful authentication.

---

## Step-by-step prompts for the AI agent

Copy these one at a time into your Android project assistant.

### 1. Data model

> Locate the customer user model and API DTOs (`User`, `UserProfile`, etc.).
> - Add nullable fields: `firstName`, `lastName`, `phoneNumber`, `address`, `country`, `birthday` (String), `dietaryPreference`.
> - Keep `id`, `username`, `email`, `role`, `status` for session use but mark `password` as `@Transient` / ignore unknown or use `@Json(ignore = true)` so it is never stored.
> - Add helper `displayName(): String` → `"$firstName $lastName".trim()` if not blank, else `username`.
> - Add helper `isProfileIncomplete(): Boolean` → true when `phoneNumber`, `address`, or `dietaryPreference` is blank (for post-Google nudge).

### 2. Retrofit / API layer

> Add or extend the user API interface:
> - `GET /users` → `User`
> - `PUT /users/profile/name` → body `UpdateNameRequest(firstName, lastName)` → `User`
> - `PUT /users/profile/phone` → body `UpdatePhoneRequest(phoneNumber)` → `User`
> - `PUT /users/profile/address` → body `UpdateAddressRequest(address)` → `User`
> - `PUT /users/profile/country` → body `UpdateCountryRequest(country)` → `User`
> - `PUT /users/profile/birthday` → body `UpdateBirthdayRequest(birthday)` → `User`
> - `PUT /users/profile/dietary-preference` → body `UpdateDietaryPreferenceRequest(dietaryPreference)` → `User`
> Reuse the existing auth interceptor. Match JSON field names exactly to the backend.

### 3. Repository

> Create or extend `UserProfileRepository`:
> - `suspend fun fetchProfile(): Result<User>` — calls `GET /users`, updates session cache.
> - `suspend fun updateName(firstName, lastName): Result<User>` — and similar for phone, address, country, birthday, dietaryPreference.
> - `suspend fun fetchProfileWithRetry(maxAttempts = 2)` — for post-Google-sign-up race with async backend user creation.
> After login (email or Google), call `fetchProfile()` before entering the main app graph.

### 4. Login integration

> In the login flow (email/password **and** Google):
> 1. Complete auth → store `accessJwt` + `refreshToken`.
> 2. Call `userProfileRepository.fetchProfileWithRetry()`.
> 3. On success, navigate to home with profile in session state.
> 4. On failure, show retry UI; do not silently proceed with an empty profile.
> For Google login use `POST /auth/oauth2/google/login` with `{ "idToken", "role": "USER" }`.

### 5. Profile ViewModel

> ProfileViewModel should:
> - On init / `onResume`, call `fetchProfile()` and expose `StateFlow<UserProfileUiState>` with loading, error, and user data.
> - Expose `updateName(...)`, `updatePhone(...)`, etc., each setting a field-level loading flag.
> - On successful update, replace state with the returned `User` from the backend.
> - Never expose or log `password`.

### 6. Profile screen UI

> Build the Profile screen showing:
> - Header: display name + username
> - Rows: Email (read-only), Phone, Address, Country, Birthday, Dietary preference
> - Empty fields → "Not set"
> - Each editable row opens a bottom sheet / edit screen
> - Link row: Favorites
> - Link row: Settings
> Use the same list-row pattern as the rest of the app (label, value, chevron).

### 7. Edit flows

> Implement edit UIs:
> - **Name**: two fields, save → `PUT /users/profile/name`
> - **Phone**: phone input, save → `PUT /users/profile/phone`
> - **Address**: multiline text, save → `PUT /users/profile/address`
> - **Country**: text or country picker, save → `PUT /users/profile/country`
> - **Birthday**: date picker, display localized, send `yyyy-MM-dd` → `PUT /users/profile/birthday`
> - **Dietary preference**: free text → `PUT /users/profile/dietary-preference`
> On save success, dismiss editor and refresh profile from response.

### 8. Post-Google profile completion (optional but recommended)

> After first Google sign-up, if `isProfileIncomplete()` is true, show a non-blocking banner or bottom sheet:
> "Complete your profile for a better experience" with shortcuts to edit Phone, Address, and Dietary preference.
> Do not block app usage.

### 9. Validation and UX

> - All profile fields optional on save.
> - Validate birthday parses as `yyyy-MM-dd` before API call.
> - Show Snackbar on network/API errors; keep previous value.
> - Disable Save + show progress while update is in flight.

### 10. Tests

> Add unit tests for:
> - JSON parsing of full `User` including null optional fields
> - `displayName()` and `isProfileIncomplete()` helpers
> - Repository calling correct endpoint per update method
> - ViewModel refresh after successful update

---

## Acceptance checklist

- [ ] After **email login**, app calls `GET /users` and stores profile.
- [ ] After **Google login**, app calls `GET /users` (with retry) and stores profile.
- [ ] Profile screen shows: name, username, email, phone, address, country, birthday, dietary preference.
- [ ] Empty fields show "Not set".
- [ ] `password`, `id`, `role`, `status` are never shown in UI.
- [ ] Each editable field saves via the correct `PUT /users/profile/...` endpoint.
- [ ] Save response updates local profile state immediately.
- [ ] Birthday sent as `yyyy-MM-dd`, displayed in user-friendly format.
- [ ] Google sign-up may pre-fill name + country; phone/address/dietary still prompt user to complete.
- [ ] Favorites and Settings are reachable from Profile but implemented separately.

---

## Related docs in this repo

- `ANDROID_GOOGLE_SIGNIN_IMPLEMENTATION.md` — Google Sign-In SDK setup
- `ANDROID_USER_PROFILE_EXTRA_FIELDS_IMPLEMENTATION_PROMPT.md` — earlier field-by-field prompt (subset of this doc)
- `ANDROID_AUTHENTICATED_PASSWORD_RESET_PROMPT.md` — change password in Settings
- `ANDROID_USER_PROFILE_DELETION_INTEGRATION_PROMPT.md` — delete account in Settings
- `MOBILE_APP_FAVORITES_INTEGRATION_GUIDE.md` — favorites screen
