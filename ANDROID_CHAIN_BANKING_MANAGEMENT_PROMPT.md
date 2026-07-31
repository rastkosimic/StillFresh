# Android Integration Prompt — Banking & Payout Management

**Scope of this file**: choosing and switching the chain banking model, setting up per-location payout
accounts, and entering / editing MoR bank details.

**Related files**
- Chain upgrade and locations → `ANDROID_CHAIN_UPGRADE_AND_LOCATIONS_PROMPT.md`
- Workers / employees → `ANDROID_EMPLOYEE_MANAGEMENT_PROMPT.md`

---

## 1. Two independent concepts — do not conflate them in the UI

**Payout model** is decided by the vendor's country and is **not** user-selectable:

| `payoutModel` | Meaning | How the vendor gets paid |
|---|---|---|
| `CONNECT` | Stripe is supported in this country | Stripe Connect express account, onboarded via a web link |
| `MOR` | Stripe is not supported (e.g. Serbia) | StillFresh is merchant of record; the vendor supplies bank details and is paid by bank transfer |

**Banking model** is chosen by the chain and applies to all of its locations:

| `usesSharedPaymentAccount` | Label | Meaning |
|---|---|---|
| `false` | `INDIVIDUAL` | Every location has its own payout account and receives its own money |
| `true` | `SHARED` | All locations are paid through the headquarters account |

The two combine: a `SHARED` + `MOR` chain is paid entirely into the HQ bank account, and a
`SHARED` + `CONNECT` chain into the HQ Stripe account. A standalone (non-chain) vendor is always
effectively `INDIVIDUAL`.

## 2. Payout readiness is the gate on selling

A location can publish offers only when it is `ACTIVE` **and** has a payout destination. The backend
resolves the destination as follows:

- `CONNECT`: a non-empty `stripeAccountId`.
- `MOR`: a non-empty IBAN **or** a non-empty account number.
- On the `SHARED` banking model, both checks are performed against the **headquarters** account, not the
  branch.

If this is not satisfied, `POST /vendors/offer-create` and `POST /vendors/update-offer/{id}` fail with
"This location has no payout account yet, so it cannot publish offers. Complete the payment account
setup first." **Do not let the user reach the offer composer in that state** — check readiness first and
show the setup call to action instead.

---

## 3. Endpoints

Base URL is the API gateway: `{BASE_URL}/vendors/...`. All endpoints require
`Authorization: Bearer <accessToken>` and the `VENDOR_ADMIN` role.

### 3.1 Chain banking overview

```
GET /vendors/chain/banking/info
```

```json
{
  "bankingModel": "INDIVIDUAL",
  "chainName": "Fresh Bakery",
  "totalLocations": 3,
  "locationsWithPaymentAccounts": 2,
  "headquartersHasAccount": true
}
```

`totalLocations` and `locationsWithPaymentAccounts` count real locations only — workers are excluded.
Render this as the header of the banking screen: `2 of 3 locations can receive payouts`, with a link to
the list of the ones that cannot.

Requires the account to be part of a chain; returns 400 "Vendor is not part of a chain" otherwise.

### 3.2 Switch the chain banking model

```
PUT /vendors/chain/banking/switch-model
```

```json
{ "bankingModel": "SHARED" }
```

`bankingModel` is `SHARED` or `INDIVIDUAL`. Success returns
`{ "success": true, "message": "..." }`.

**Headquarters only.** A branch admin gets "Only Headquarters VENDOR_ADMIN can switch banking model.
Please contact your chain headquarters administrator to request this change."

**Switching to `SHARED`** requires headquarters to already have a payout destination, otherwise the call
fails with "Headquarters must have a payment account before switching to SHARED model." Check
`headquartersHasAccount` from 3.1 and disable the option with an explanatory caption when it is false.

**Switching to `INDIVIDUAL` invalidates every offer of every location in the chain.** This is
intentional: each location must set up its own account before it can sell again. This is destructive and
irreversible in the sense that the offers are gone, so require an explicit typed or two-step
confirmation, not a single tap. The confirmation must state:

> All active offers across all N locations will be removed. Each location must set up its own payout
> account before it can publish offers again.

Switching to the model that is already active is a no-op and returns success without changes.

All locations are emailed and receive a push notification about the change, so do not build your own
fan-out notification.

### 3.3 Set up a payout account for one location

```
POST /vendors/chain/locations/{locationId}/setup-payment-account
```

Used on the `INDIVIDUAL` model. Headquarters may call it for any location in the chain; a branch admin
only for its own `locationId`. Initialises the account for the location's `payoutModel` — a Stripe
Connect account for `CONNECT`, or the MoR record for `MOR`.

Once the account becomes usable, the location's onboarding status moves to `COMPLETED` automatically.
Re-fetch the location list after this call so the readiness badges update.

For `CONNECT` locations, initialising the account is only step one — the vendor must still complete
Stripe's hosted onboarding. Follow it with 3.4.

For `MOR` locations, this call alone does **not** store IBAN/holder details. Prefer opening the MoR
bank form and calling **3.6b** (`PUT .../locations/{locationId}/mor/bank-details`), which initialises
MoR if needed and writes the bank details in one step. Keep 3.3 for Stripe-only init or when you need
to create the empty MoR shell without details yet.

### 3.4 Payment account status and Stripe onboarding link

```
GET  /vendors/payment/status
POST /vendors/payment/onboarding-link
```

`GET /vendors/payment/status` describes the **authenticated account**:
```json
{
  "isReady": false,
  "hasAccount": true,
  "provider": "MOR",
  "payoutModel": "MOR",
  "accountId": "hq@freshbakery.rs",
  "country": "RS",
  "stripeSupported": false,
  "balance": 12450.00,
  "manualPayoutMethod": "BANK_TRANSFER",
  "hasBankDetails": true,
  "message": "Your payment account is not ready. Please complete onboarding."
}
```

`balance`, `manualPayoutMethod` and `hasBankDetails` are present only when `provider == "MOR"`. Use
`isReady` for the primary gate and `provider` to decide which setup flow to show.

`POST /vendors/payment/onboarding-link` returns `{ "onboardingUrl": "...", "message": "..." }`. Open it
in a Custom Tab, not a WebView, because it is a Stripe-hosted flow. On return, poll
`GET /vendors/payment/status` (or `GET /vendors/stripe/account-status`) until `isReady` is true — the
link completing does not guarantee Stripe has finished verification.

### 3.5 Read MoR bank details (authenticated account)

```
GET /vendors/mor/bank-details
```

```json
{
  "hasBankDetails": true,
  "holderName": "Fresh Bakery d.o.o.",
  "bankName": "Raiffeisen Banka",
  "swiftCode": "RZBSRSBG",
  "accountNumberMasked": "**********4321",
  "ibanMasked": "******************3000",
  "manualPayoutMethod": "BANK_TRANSFER"
}
```

`MOR` payout model only; returns 400 "Vendor is not using MoR model" for `CONNECT` vendors.
Applies to the **authenticated** vendor (standalone, HQ self, or a branch admin reading their own
account). For HQ reading/writing **another** location on `INDIVIDUAL`, use 3.5b / 3.6b instead.

**The IBAN and account number are returned masked and the raw values are never available to the
client.** This has a direct UI consequence: you cannot pre-fill those two inputs for editing. Show the
masked value as read-only text with an "Update" action that opens an empty field, and label it clearly
("Enter the full IBAN to replace the current one"). Use `hasBankDetails` to choose between an "Add bank
details" and "Update bank details" entry point.

### 3.5b Read MoR bank details for a chain location

```
GET /vendors/chain/locations/{locationId}/mor/bank-details
```

Same response shape as 3.5. Used when headquarters (or the branch itself) needs bank details for a
specific location on the **INDIVIDUAL** banking model.

| Caller | Allowed `locationId` |
|---|---|
| Headquarters `VENDOR_ADMIN` | Any location in the same chain |
| Branch `VENDOR_ADMIN` | Own location id only |
| Worker `VENDOR` | ✗ (403 / role gate) |

**Rejects with 400 when:**
- The location uses `SHARED` banking — bank details live on headquarters; use 3.5 on the HQ session.
- The location is not MoR (`Vendor is not using MoR model`).
- The location is on a different chain, or is a worker row.

Re-fetch this after a successful 3.6b write so the readiness badge and masked values update.

### 3.6 Submit or update MoR bank details (authenticated account)

```
PUT /vendors/mor/bank-details
```

```json
{
  "holderName": "Fresh Bakery d.o.o.",
  "bankName": "Raiffeisen Banka",
  "iban": "RS35260005601001611379",
  "accountNumber": "265104031000012345",
  "swiftCode": "RZBSRSBG",
  "payoutMethod": "BANK_TRANSFER"
}
```

**This endpoint is a true partial update: only the keys present in the JSON object are applied.** Omit a
key to leave the stored value untouched; send it as an empty string to clear it (except `holderName`,
which cannot be blanked). This is what makes the masked-read / write-only pattern in 3.5 workable —
sending only `{"iban": "..."}` will not wipe the holder name or SWIFT code.

Server-side validation, all of which you should mirror client-side to avoid a round trip:

| Field | Rule |
|---|---|
| `holderName` | If present, must not be blank. Required overall before the update can succeed. |
| `iban` | Whitespace **and hyphens** stripped, then upper-cased. Must match 2 country letters, 2 check digits, then 11–30 alphanumerics, **and** pass the ISO 13616 mod-97 checksum. Clients may send `RS35-...` or spaced groups; the server stores the continuous form. |
| `accountNumber` | Whitespace stripped and upper-cased. Hyphens are kept (Serbian `xxx-xxxxxxxxx-xx` format). 5–34 characters, `A–Z`, `0–9` and `-` only. |
| `swiftCode` | Whitespace **and hyphens** stripped, then upper-cased. 8 or 11 characters: 6 letters then 2 alphanumerics, optionally 3 more. |
| `payoutMethod` | Must match a `ManualPayoutMethod` enum value; case-insensitive. |
| overall | At least one of `iban` / `accountNumber` must end up non-empty, and `holderName` must be set. |

Implement the mod-97 check in the app so a typo is caught before submitting — the alternative is a
payment file rejected by the bank days later:

```kotlin
fun isValidIban(raw: String): Boolean {
    val iban = raw.filterNot { it.isWhitespace() }.uppercase()
    if (!Regex("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$").matches(iban)) return false
    val rearranged = iban.substring(4) + iban.substring(0, 4)
    val numeric = buildString {
        rearranged.forEach { c ->
            if (c.isDigit()) append(c) else append(c - 'A' + 10)
        }
    }
    return numeric.fold(0) { acc, c -> (acc * 10 + (c - '0')) % 97 } == 1
}
```

**Changing an existing payout destination triggers a security email to the vendor.** Tell the user this
will happen in the confirmation step — it is a deliberate anti-account-takeover measure, and a user who
is surprised by the email will contact support. No email is sent the first time details are added.

### 3.6b Submit or update MoR bank details for a chain location

```
PUT /vendors/chain/locations/{locationId}/mor/bank-details
```

Body is the **same** partial-update shape and validation as 3.6.

**Who may call it**

| Caller | Allowed `locationId` |
|---|---|
| Headquarters `VENDOR_ADMIN` | Any location in the same chain (including HQ itself) |
| Branch `VENDOR_ADMIN` | Own location id only |
| Worker `VENDOR` | ✗ |

**Behaviour**

1. Requires `INDIVIDUAL` banking on that location. On `SHARED`, returns 400 telling the client to manage
   bank details on headquarters instead.
2. If the location is not yet MoR-initialised, the server initialises the MoR payout model (same as
   `POST .../setup-payment-account`) before applying the body — HQ can finish bank details in one
   step without a prior empty shell call.
3. When a payout destination exists after the write, the location's `onboardingStatus` moves to
   `COMPLETED` so it can publish offers.
4. Security email on destination **change** is sent to the **location's** email (not the HQ admin who
   submitted the form).

Success (200):
```json
{ "success": true, "message": "Bank details submitted successfully for location" }
```

**Android UI implication:** from the locations list / banking overview, a "Set up payments" / "Add bank
details" action on a not-ready MoR location must open the MoR bank form and call **3.6b** with that
`locationId` — do not only call `setup-payment-account`, and do not call self `PUT /vendors/mor/bank-details`
while logged in as HQ expecting it to update the branch.

### 3.7 MoR balance, transactions and payouts

```
GET /vendors/mor/balance
GET /vendors/mor/transactions
GET /vendors/mor/payouts
```

Read-only history screens. Payouts for MoR vendors are batched and executed automatically by the
platform, so build these as informational lists. Do not build a "request payout" button — the manual
request path is deprecated in favour of the automated ledger pipeline.

---

## 4. Kotlin contracts

```kotlin
enum class BankingModel { SHARED, INDIVIDUAL }

data class SwitchBankingModelRequest(val bankingModel: BankingModel)

data class BankingInfo(
    val bankingModel: String,
    val chainName: String?,
    val totalLocations: Int,
    val locationsWithPaymentAccounts: Int,
    val headquartersHasAccount: Boolean
)

data class PaymentAccountStatus(
    val isReady: Boolean,
    val hasAccount: Boolean,
    val provider: String,          // "STRIPE" | "MOR"
    val payoutModel: String?,      // "CONNECT" | "MOR"
    val accountId: String?,
    val country: String?,
    val stripeSupported: Boolean,
    val message: String,
    val balance: java.math.BigDecimal? = null,
    val manualPayoutMethod: String? = null,
    val hasBankDetails: Boolean? = null
)

data class MorBankDetails(
    val hasBankDetails: Boolean,
    val holderName: String? = null,
    val bankName: String? = null,
    val swiftCode: String? = null,
    val accountNumberMasked: String? = null,
    val ibanMasked: String? = null,
    val manualPayoutMethod: String? = null
)

/** Null fields are omitted from the request body so the server leaves them unchanged. */
data class BankDetailsUpdate(
    val holderName: String? = null,
    val bankName: String? = null,
    val iban: String? = null,
    val accountNumber: String? = null,
    val swiftCode: String? = null,
    val payoutMethod: String? = null
)
```

Configure the serializer to omit nulls for `BankDetailsUpdate` (Moshi omits them by default; with Gson
this is also the default — verify, because serialising nulls here would blank stored fields).

```kotlin
interface BankingApi {

    @GET("vendors/chain/banking/info")
    suspend fun getBankingInfo(): Response<BankingInfo>

    @PUT("vendors/chain/banking/switch-model")
    suspend fun switchBankingModel(@Body request: SwitchBankingModelRequest): Response<ApiResponse>

    @POST("vendors/chain/locations/{locationId}/setup-payment-account")
    suspend fun setupLocationPaymentAccount(@Path("locationId") locationId: Long): Response<ApiResponse>

    @GET("vendors/chain/locations/{locationId}/mor/bank-details")
    suspend fun getLocationBankDetails(
        @Path("locationId") locationId: Long
    ): Response<MorBankDetails>

    @PUT("vendors/chain/locations/{locationId}/mor/bank-details")
    suspend fun updateLocationBankDetails(
        @Path("locationId") locationId: Long,
        @Body body: BankDetailsUpdate
    ): Response<ApiResponse>

    @GET("vendors/payment/status")
    suspend fun getPaymentStatus(): Response<PaymentAccountStatus>

    @POST("vendors/payment/onboarding-link")
    suspend fun getOnboardingLink(): Response<Map<String, String>>

    @GET("vendors/mor/bank-details")
    suspend fun getBankDetails(): Response<MorBankDetails>

    @PUT("vendors/mor/bank-details")
    suspend fun updateBankDetails(@Body body: BankDetailsUpdate): Response<ApiResponse>
}
```

---

## 5. Error handling

| Server message contains | UI treatment |
|---|---|
| `Only Headquarters VENDOR_ADMIN can switch banking model` | Hide the switch for branch admins; show a read-only model with a note to contact HQ. |
| `Headquarters must have a payment account before switching to SHARED` | Disable `SHARED` and link to HQ payment setup. |
| `Vendor is not part of a chain` | Standalone vendor: show the single-account banking screen instead. |
| `Vendor is not using MoR model` | This vendor is on Stripe. Show the Stripe screen; the MoR screen should be unreachable. |
| `uses shared payment account` / `shared payment account` | On location bank-details: navigate to HQ banking / explain SHARED. Hide per-location bank form. |
| `Only headquarters can` / `You can only` (bank details) | Branch tried another location — hide the control and refresh identity. |
| `different chain` | Stale cache: re-fetch locations. |
| `IBAN format is invalid` | Inline error on the IBAN field. Keep the input. |
| `IBAN checksum is invalid` | Inline error: "Please double-check the IBAN — one of the characters looks wrong." |
| `SWIFT/BIC format is invalid` | Inline error on the SWIFT field. |
| `Account number format is invalid` | Inline error on the account number field. |
| `Either an IBAN or an account number is required` | Form-level error above the submit button. |
| `Account holder name is required` | Inline error on the holder name field. |
| `Invalid payout method` | Your enum is out of sync with the backend. Fall back to omitting the field. |

## 6. Screens to build

1. **Banking overview** (chain) — current model, the `x of y locations can receive payouts` counter, a
   list of locations that are not ready with a per-row setup action, and the model switch.
2. **Model switch confirmation** — two-step, with the offer-invalidation warning for `INDIVIDUAL`.
3. **Payout account setup** — branches on `provider`: Stripe Custom Tab flow, or the MoR bank details
   form. For a **chain location** on MoR + INDIVIDUAL, the form must call
   `PUT /vendors/chain/locations/{locationId}/mor/bank-details` (3.6b), not the self endpoint.
4. **MoR bank details** — masked read view plus a write-only update form with client-side IBAN, SWIFT
   and account-number validation, and the "we will email you about this change" notice. Accept an
   optional `locationId` param: when set, use 3.5b/3.6b; when absent, use 3.5/3.6 (self).
5. **Balance / transactions / payouts** — read-only history, MoR only.

## 7. Acceptance checklist

- [ ] Offer creation is gated on payout readiness before the composer opens.
- [ ] `SHARED` is disabled unless `headquartersHasAccount` is true.
- [ ] Switching to `INDIVIDUAL` requires a two-step confirmation naming the offer loss.
- [ ] The banking model switch is hidden for branch admins.
- [ ] Bank-detail requests omit null fields so a partial update cannot blank stored values.
- [ ] IBAN mod-97 is validated client-side before submitting.
- [ ] Masked IBAN / account number are never used to pre-fill an editable input.
- [ ] Raw bank values are never written to logs, analytics or crash reports.
- [ ] The user is told that changing the payout destination sends a security email.
- [ ] Stripe onboarding return polls `isReady` instead of assuming success.
- [ ] No "request manual payout" control is built.
- [ ] HQ can add/update MoR bank details for a branch via `PUT .../locations/{id}/mor/bank-details`.
- [ ] Branch admin can add/update MoR bank details for their own location via the same endpoint.
- [ ] Workers never see banking or worker-management entry points.
- [ ] Location "Set up payments" for MoR opens the bank form and saves via 3.6b (not init-only).
