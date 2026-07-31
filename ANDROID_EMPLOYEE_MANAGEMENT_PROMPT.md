# Android Integration Prompt — Employee (Worker) Management

**Scope of this file**: creating, listing, editing, activating, deactivating and deleting the staff
accounts that operate a location.

**Related files**
- Chain upgrade and locations → `ANDROID_CHAIN_UPGRADE_AND_LOCATIONS_PROMPT.md`
- Banking model, bank details and payout readiness → `ANDROID_CHAIN_BANKING_MANAGEMENT_PROMPT.md`

---

## 1. What a worker is

A worker (called an *employee* in the UI) is a login account with the `VENDOR` role and a non-null
`assignedLocationId`. It lets staff publish and manage offers for exactly one location without giving
them access to the business account.

```
vendor #204  role=VENDOR_ADMIN  assignedLocationId=null  → the "Novi Sad" location itself
vendor #311  role=VENDOR        assignedLocationId=204   → a worker of "Novi Sad"
vendor #312  role=VENDOR        assignedLocationId=204   → another worker of "Novi Sad"
```

Workers are stored in the same table as locations and inherit the location's `chainId`, address,
coordinates, country, business type and banking flags at creation time. **The distinguishing field is
`assignedLocationId`, not the role alone** — never infer "is a location" from anything else.

| Capability | `VENDOR_ADMIN` (location) | `VENDOR` (worker) |
|---|---|---|
| Create / edit offers for its location | ✓ | ✓ |
| Manage locations | ✓ | ✗ |
| Manage workers | ✓ | ✗ |
| Banking and bank details | ✓ | ✗ |

A worker's offers are attributed to their assigned location, not to the worker, so the customer app is
unaffected by which staff member created a listing.

Standalone (non-chain) vendors can have workers too. In that case the only valid `locationId` is the
vendor's own id.

---

## 2. Permission matrix — implement this client-side

| Action | Headquarters `VENDOR_ADMIN` | Branch `VENDOR_ADMIN` | Standalone `VENDOR_ADMIN` | Worker |
|---|---|---|---|---|
| List workers of a location | ✓ any location in chain | ✓ own location only | ✓ itself only | ✗ |
| Create worker | ✓ any location in chain | ✓ own location only | ✓ itself only | ✗ |
| Update worker | ✓ any | ✓ own location's workers | ✓ | ✗ |
| Reassign worker to another location | ✓ | ✗ | n/a | ✗ |
| Activate / deactivate worker | ✓ any | ✓ own location's workers | ✓ | ✗ |
| Delete worker | ✓ any | ✓ own location's workers | ✓ | ✗ |

Reassignment is HQ-only because it requires permission over both the source and the destination
location. A branch admin attempting it gets a 400.

There is a cap of **100 workers per location** (configurable server-side). Surface the current count so
the limit is not a surprise.

---

## 3. Endpoints

Base URL is the API gateway: `{BASE_URL}/vendors/...`. All endpoints require
`Authorization: Bearer <accessToken>` and the `VENDOR_ADMIN` role.

### 3.1 List a location's workers

```
GET /vendors/chain/locations/{locationId}/workers
```

Returns a JSON array of vendor objects filtered to the `VENDOR` role. Read `id`, `username`, `email`,
`phone`, `status`, `assignedLocationId`, `locationName`.

Show `status` as an Active / Inactive chip. The list is per location, so on a chain the workers screen
needs a location selector (HQ) or is fixed to the caller's own location (branch admin).

### 3.2 Create a worker

```
POST /vendors/chain/locations/{locationId}/workers
```

```json
{
  "username": "milan.novisad",
  "email": "milan@freshbakery.rs",
  "password": "TempPass123",
  "phone": "+381601234567"
}
```

| Field | Rule |
|---|---|
| `username` | Required, 3–50 characters. Must be globally unique — it is a login identifier. |
| `email` | Required, valid email, must not already be registered. |
| `password` | Required, minimum 6 characters. |
| `phone` | Optional. Defaults to the location's phone number. |

Success (200):
```json
{ "success": true, "message": "Worker created successfully. Credentials sent to: milan@freshbakery.rs" }
```

The worker is created `ACTIVE` with onboarding already `COMPLETED`, so they can log in immediately. The
credentials are emailed to them.

**The password is chosen by the admin creating the account and is sent as plaintext in
`WorkerRequest.password`.** The backend BCrypt-encodes it and stores the hash in both vendor-service
and authorization-service. Login uses the authorization-service hash.

Do **not** pre-hash the password on the client. If you send a hash, the server will hash it again and
login with the plaintext the admin typed will fail.

The create response does not echo the password. Note or share it yourself, or use the in-app
"Generiši" helper and the one-time display. Unlike location creation, there is no fallback copy if the
credentials email fails.

Do not create workers for a location that is not yet able to sell — the worker would be able to log in
but every offer attempt would be rejected. Check the location's payout readiness first (see the banking
file) and warn the admin.

### 3.3 Update a worker

```
PUT /vendors/chain/workers/{workerId}
```

```json
{
  "username": "milan.ns",
  "phone": "+381601111222",
  "assignedLocationId": 205
}
```

**All three fields are optional and only the provided ones are applied**, so send just what changed.
A blank or absent value is ignored rather than written.

- `username` — 3–50 characters, must still be globally unique.
- `phone` — free text.
- `assignedLocationId` — **headquarters only**. Moves the worker to another location of the same chain
  and re-copies that location's address, coordinates, country, business type and banking flags onto the
  worker. The target must be a real location, not another worker.

Email is not editable here: it is the login identifier. To change it, delete the worker and create a new
one.

Success returns `{ "success": true, "message": "Worker updated successfully" }`.

### 3.4 Activate / deactivate a worker

```
PUT /vendors/chain/workers/{workerId}/activate
PUT /vendors/chain/workers/{workerId}/deactivate
```

Deactivation sets the worker's status to `INACTIVE` **and revokes their access immediately** — the change
is propagated to the authorization service, their refresh-token sessions are revoked, and the gateway
rejects further requests even if their existing access token has not expired yet.

This makes deactivation the correct action for staff who leave, and it is reversible. Present it more
prominently than deletion.

The location's offers are **not** affected by deactivation, because offers belong to the location rather
than to the worker who created them. State this in the confirmation so the admin does not assume
otherwise.

### 3.5 Delete a worker

```
DELETE /vendors/chain/workers/{workerId}
```

A hard delete. It removes the vendor record **and** the login account in the authorization service, which
frees the email address for reuse.

The location's offers are unaffected, because offers are attributed to the location rather than to the
staff member who created them.

Because it is irreversible, the confirmation dialog must be distinct from deactivation:

> This permanently deletes the account. Deactivate instead if they might return.

---

## 4. Kotlin contracts

```kotlin
data class WorkerRequest(
    val username: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

/** Null fields are omitted so the server leaves them unchanged. */
data class WorkerUpdateRequest(
    val username: String? = null,
    val phone: String? = null,
    val assignedLocationId: Long? = null
)

data class WorkerDto(
    val id: Long,
    val username: String,
    val email: String,
    val phone: String?,
    val status: String,               // "ACTIVE" | "INACTIVE"
    val assignedLocationId: Long?,
    val locationName: String?
)
```

```kotlin
interface WorkerApi {

    @GET("vendors/chain/locations/{locationId}/workers")
    suspend fun getWorkers(@Path("locationId") locationId: Long): Response<List<WorkerDto>>

    @POST("vendors/chain/locations/{locationId}/workers")
    suspend fun createWorker(
        @Path("locationId") locationId: Long,
        @Body request: WorkerRequest
    ): Response<ApiResponse>

    @PUT("vendors/chain/workers/{workerId}")
    suspend fun updateWorker(
        @Path("workerId") workerId: Long,
        @Body request: WorkerUpdateRequest
    ): Response<ApiResponse>

    @PUT("vendors/chain/workers/{workerId}/activate")
    suspend fun activateWorker(@Path("workerId") workerId: Long): Response<ApiResponse>

    @PUT("vendors/chain/workers/{workerId}/deactivate")
    suspend fun deactivateWorker(@Path("workerId") workerId: Long): Response<ApiResponse>

    @DELETE("vendors/chain/workers/{workerId}")
    suspend fun deleteWorker(@Path("workerId") workerId: Long): Response<ApiResponse>
}
```

---

## 5. Error handling

| Server message contains | UI treatment |
|---|---|
| `Username already taken` | Inline error on the username field. Keep the rest of the form. |
| `Email already registered` | Inline error on the email field. Note that a previously deleted worker's email is reusable, so this means a live account. |
| `maximum of N workers` | Dialog telling the user to contact support. Disable "Add employee" for that location. |
| `Only headquarters can create workers for other locations` | The location picker offered a location the caller cannot manage. Restrict it to the caller's own location. |
| `Only headquarters can reassign workers` (or `reassign`) | Hide the location field in the edit form for branch admins. |
| `different chain` | Stale cache: re-fetch the location list. |
| `Account is not a worker (VENDOR role)` | A location id was passed where a worker id was expected. Bug in your navigation arguments. |
| `Worker is not assigned to a location` | Data inconsistency; report it and refresh the list. |
| `Target account is a worker, not a location` | The reassignment picker included a worker. Filter to `assignedLocationId == null`. |
| `Workers cannot own workers` | Same cause as above, on the create path. |

Also handle `403 Account is not active` on any request: the signed-in account was deactivated while its
token was still valid. Clear the session and return to login with "Your account is no longer active."
This is exactly what a deactivated worker will experience on their own device, so make sure the message
is friendly rather than a generic error.

## 6. Screens to build

1. **Employees list** — scoped to one location. For HQ, a location selector at the top; for a branch
   admin, fixed to their own location with no selector. Show `n / 100 employees`. Active and inactive
   are in one list, visually distinguished.
2. **Add employee** — username / email / password / optional phone, with a "generate password" helper
   and a one-time display of the chosen password. Block submission when the location is not payout-ready
   and explain why.
3. **Edit employee** — username and phone always; a location picker only for HQ, populated exclusively
   with rows where `assignedLocationId == null`.
4. **Row actions** — Deactivate (primary, reversible) and Delete (destructive, in an overflow menu with
   a distinct confirmation).

## 7. Acceptance checklist

- [ ] Worker lists are always scoped to a `locationId` and never mix locations.
- [ ] Branch admins see no location selector and cannot reach other locations' workers.
- [ ] The reassignment picker only offers real locations (`assignedLocationId == null`).
- [ ] Update requests omit unchanged fields.
- [ ] The admin-chosen password is shown once at creation with a copy or share action.
- [ ] Creating a worker for a location that cannot sell yet is blocked or clearly warned about.
- [ ] Deactivate is presented as the default off-boarding action; Delete has its own stronger,
      clearly irreversible confirmation.
- [ ] Both confirmations state that the location's offers stay live.
- [ ] `403 Account is not active` clears the session with a human-readable message.
- [ ] The employee count against the 100-per-location cap is visible.
