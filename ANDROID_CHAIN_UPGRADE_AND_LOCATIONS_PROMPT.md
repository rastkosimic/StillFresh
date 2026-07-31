# Android Integration Prompt — Chain Upgrade & Location Management

**Scope of this file**: turning a single-location vendor into a chain, and adding / editing / removing the
locations of that chain.

**Related files**
- Banking model, bank details and payout readiness → `ANDROID_CHAIN_BANKING_MANAGEMENT_PROMPT.md`
- Workers / employees → `ANDROID_EMPLOYEE_MANAGEMENT_PROMPT.md`

---

## 1. Domain model you must understand before writing code

A chain is **not** a parent row with child rows. Every location — including headquarters — is its own
vendor account with its own login, and they are tied together by a shared `chainId`.

```
chainId = "8f3c...":
  vendor #101  isHeadquarters=true   locationName="Belgrade Centre"   role=VENDOR_ADMIN
  vendor #204  isHeadquarters=false  locationName="Novi Sad"          role=VENDOR_ADMIN
  vendor #205  isHeadquarters=false  locationName="Niš"               role=VENDOR_ADMIN
```

Workers live in the same table and also carry the `chainId`, but they have a non-null
`assignedLocationId`. **A row is a location if and only if `assignedLocationId == null`.** The backend
already filters workers out of `GET /vendors/chain/locations`, so do not re-derive location lists from
any other endpoint.

Three flags drive all UI branching:

| Field | Meaning |
|---|---|
| `isUniqueVendor` | Standalone single-location vendor. May upgrade to a chain. |
| `isChainLocation` | This account belongs to a chain (HQ or branch). |
| `isHeadquarters` | This account is the chain's HQ. Only HQ may perform chain-wide actions. |

Treat a `null` value for any of these as `false`, except `isUniqueVendor`, where `null` means "legacy
standalone vendor" and should be treated as `true`.

---

## 2. Permission matrix — implement this client-side, do not rely on the server error alone

| Action | Headquarters `VENDOR_ADMIN` | Branch `VENDOR_ADMIN` | Worker `VENDOR` |
|---|---|---|---|
| Upgrade to chain | n/a (already a chain) | ✗ | ✗ |
| Add location | ✓ | ✗ | ✗ |
| List locations | ✓ | ✓ | ✗ |
| Update location | ✓ (any branch) | ✓ (own only) | ✗ |
| Remove location | ✓ (never HQ, never self) | ✗ | ✗ |

Hide or disable the action entirely when the current account is not permitted. A branch admin should
never see an "Add location" button — the request will fail with a 400 and a message telling them to
contact headquarters, which is a poor experience if it was reachable at all.

---

## 3. Endpoints

Base URL is the API gateway: `{BASE_URL}/vendors/...`. Every endpoint below requires
`Authorization: Bearer <accessToken>`.

### 3.1 Upgrade a standalone vendor to a chain

```
POST /vendors/upgrade-to-chain?chainName=Fresh%20Bakery
```

**Note the chain name is a query parameter, not a JSON body.** This is the single most common
integration mistake with this endpoint.

Success (200):
```json
{ "success": true, "message": "Vendor upgraded to chain successfully. You can now add multiple locations." }
```

**Preconditions enforced by the backend** — check all of them before enabling the button:

1. Role must be `VENDOR_ADMIN`.
2. The account must not already be part of a chain (`isChainLocation != true`).
3. `onboardingStatus` must be `COMPLETED`. A vendor mid-onboarding cannot upgrade, because the chain
   flow expects a headquarters step that is no longer reachable once the type was set to `UNIQUE`.
4. The chain name must not already be used by a different chain.

**Server side-effects you must reflect in the UI:**
- A `chainId` is generated and the account becomes its own headquarters (`isHeadquarters = true`).
- `isUniqueVendor` flips to `false`.
- The banking model defaults to `INDIVIDUAL` (`usesSharedPaymentAccount = false`).
- Existing live offers are re-published with the new chain name, so the customer app will show the new
  brand within seconds. Warn the user about this in the confirmation dialog.

After a successful call, **re-fetch the vendor profile and `GET /vendors/onboarding/status`** rather
than mutating local state, so `chainId` and `isHeadquarters` come from the server.

### 3.2 Add a location

```
POST /vendors/chain/locations
```

Request body:
```json
{
  "locationName": "Novi Sad",
  "email": "novisad@freshbakery.rs",
  "phone": "+381601234567",
  "address": "Zmaj Jovina 12",
  "zipCode": "21000",
  "latitude": 45.2671,
  "longitude": 19.8335,
  "country": "Serbia"
}
```

`locationName`, `email`, `phone`, `address`, `latitude` and `longitude` are required. `zipCode` and
`country` are optional; `country` accepts either a country name or an ISO-2 code and is normalised
server-side. If omitted, the chain default is used.

Response (200) — `LocationCreationResponse`:
```json
{
  "locationId": 204,
  "locationName": "Novi Sad",
  "email": "novisad@freshbakery.rs",
  "emailSent": true,
  "emailError": null,
  "username": null,
  "password": null,
  "paymentAccountReady": false,
  "message": "Location added successfully. Credentials sent to: novisad@freshbakery.rs This location still needs a payout account before it can publish offers - use the location payment setup endpoint to finish it."
}
```

**Handle these two response fields carefully — they change what the user must do next:**

- **`emailSent == false`**: the location account exists, but the credentials email failed. In this case
  `username` and `password` are populated in the response and are the **only** copy of that password.
  Show them in a dismissible-once dialog with a copy button and an explicit warning that they will not
  be shown again. Read `emailError` into a secondary line of text. Never log these values.
- **`paymentAccountReady == false`**: the location cannot publish offers yet. Show a persistent warning
  on the location row and deep-link to the payment setup flow described in
  `ANDROID_CHAIN_BANKING_MANAGEMENT_PROMPT.md` (for MoR + INDIVIDUAL: open the bank form and call
  `PUT /vendors/chain/locations/{locationId}/mor/bank-details` — do not stop at setup-payment-account
  alone). Do not present the location as fully set up.

The new location's onboarding status is `BANKING_SETUP` while `paymentAccountReady` is false and
`COMPLETED` once it is true.

### 3.3 List locations

```
GET /vendors/chain/locations
```

Returns a JSON array of vendor objects. Workers are already excluded. Read at minimum:
`id`, `locationName`, `email`, `phone`, `address`, `zipCode`, `latitude`, `longitude`, `country`,
`status`, `isHeadquarters`, `onboardingStatus`, `payoutModel`, `usesSharedPaymentAccount`.

Sort headquarters first, then the rest alphabetically by `locationName`. Badge each row with its
`status` (`ACTIVE` / `INACTIVE`) and, for HQ, a "Headquarters" chip.

### 3.4 Update a location

```
PUT /vendors/chain/locations/{locationId}
```

Body is the same `LocationRequest` shape as 3.2.

**Two things about this body will bite you:**

1. **It is a full replacement, not a patch.** `locationName`, `phone`, `address`, `zipCode`, `latitude`
   and `longitude` are written from the request unconditionally, so pre-fill the form with the current
   values and always submit the complete object. Submitting a partial body blanks the omitted fields.
2. **`email` is still required by bean validation even though the update ignores it.** Omitting it
   returns 400 "Validation failed: Email cannot be blank". Send the location's current email; it will
   not change the login identifier. Do not expose it as an editable field — changing a location's login
   email is not supported by this endpoint.

Headquarters **cannot** be updated through this endpoint — the server rejects it with "Cannot update
headquarters using this endpoint. Use profile update instead." Route the HQ row to
`PUT /vendors/update-profile` instead.

A successful update re-publishes the location's live offers, so the customer app picks up the new name,
address and coordinates. Mention this in the save confirmation.

### 3.5 Remove a location

```
DELETE /vendors/chain/locations/{locationId}
```

This is a **soft delete**: the location's status becomes `INACTIVE`. The account is not erased.

Cascading effects to state plainly in the confirmation dialog before the user proceeds:
- All of that location's offers are invalidated immediately.
- Every worker assigned to that location is deactivated and can no longer log in.
- The location's own login is disabled.

Headquarters cannot be removed, and a location cannot remove itself.

---

## 4. Kotlin contracts

```kotlin
data class LocationRequest(
    val locationName: String,
    val email: String,                // required on create AND update (ignored when updating)
    val phone: String,
    val address: String,
    val zipCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null
)

data class LocationCreationResponse(
    val locationId: Long,
    val locationName: String,
    val email: String,
    val emailSent: Boolean,
    val emailError: String? = null,
    val username: String? = null,     // present only when emailSent == false
    val password: String? = null,     // present only when emailSent == false
    val paymentAccountReady: Boolean,
    val message: String
)

data class ApiResponse(val success: Boolean, val message: String)
data class ErrorResponse(val message: String)
```

```kotlin
interface ChainApi {

    @POST("vendors/upgrade-to-chain")
    suspend fun upgradeToChain(@Query("chainName") chainName: String): Response<ApiResponse>

    @POST("vendors/chain/locations")
    suspend fun addLocation(@Body request: LocationRequest): Response<LocationCreationResponse>

    @GET("vendors/chain/locations")
    suspend fun getLocations(): Response<List<VendorDto>>

    @PUT("vendors/chain/locations/{locationId}")
    suspend fun updateLocation(
        @Path("locationId") locationId: Long,
        @Body request: LocationRequest
    ): Response<ApiResponse>

    @DELETE("vendors/chain/locations/{locationId}")
    suspend fun removeLocation(@Path("locationId") locationId: Long): Response<ApiResponse>
}
```

---

## 5. Error handling

Every failure path returns HTTP 400 (business rule) or 500 (unexpected) with
`{ "message": "..." }`. The message is written for end users and can be surfaced directly, but map the
cases below to specific UI so the user knows what to do:

| Server message contains | UI treatment |
|---|---|
| `already part of a chain` | Upgrade screen should not have been reachable. Refresh profile and navigate to chain management. |
| `Finish onboarding before upgrading to a chain` | Deep-link to the onboarding flow at the reported status. |
| `already in use` (chain name) | Inline field error on the chain-name input. Keep the entered text. |
| `Email already registered` | Inline field error on the location email input. |
| `maximum of N locations` | Non-dismissible dialog telling the user to contact support. Disable "Add location". |
| `Only headquarters can` | Explain the account is a branch and name the action HQ must perform. Hide the control afterwards. |
| `different chain` | Treat as a bug or stale cache: clear the location list and re-fetch. |
| `Cannot update headquarters using this endpoint` | Route to the profile screen instead. |
| `Cannot remove headquarters` / `Cannot remove your own location` | Disable the delete affordance for those rows up front. |
| `Chain ID is missing` | Chain setup is incomplete. Send the user to onboarding. |

Also handle `403 Account is not active`: this comes from the gateway trust filter and means the
account was deactivated while the token was still valid. Clear the session and return to login with
the message "Your account is no longer active."

---

## 6. Screens to build

1. **Upgrade to chain** — single text field for the chain name, a summary of what changes (HQ
   designation, individual banking model by default, offers re-branded), and a confirm button gated on
   the four preconditions in 3.1.
2. **Locations list** — HQ pinned first, status and payout-readiness badges, "Add location" visible only
   to HQ, per-row overflow menu (Edit / Manage workers / Set up payments / Remove).
3. **Add / edit location form** — shared form, with a map picker for the coordinates since latitude and
   longitude are mandatory on create. Validate presence client-side before calling.
4. **Credentials fallback dialog** — shown only when `emailSent == false`, as described in 3.2.

## 7. Acceptance checklist

- [ ] `chainName` is sent as a query parameter on upgrade.
- [ ] Upgrade is blocked unless `onboardingStatus == COMPLETED`.
- [ ] Profile and onboarding status are re-fetched after a successful upgrade.
- [ ] Location list never renders worker accounts.
- [ ] Add-location handles `emailSent == false` by surfacing the one-time credentials.
- [ ] Locations with `paymentAccountReady == false` are visibly incomplete and link to payment setup.
- [ ] Update sends the complete `LocationRequest` including the current `email`, pre-filled from current
      values.
- [ ] HQ row routes to profile update, not the location update endpoint.
- [ ] Remove shows a confirmation naming the offer and worker consequences.
- [ ] Branch admins do not see HQ-only controls.
