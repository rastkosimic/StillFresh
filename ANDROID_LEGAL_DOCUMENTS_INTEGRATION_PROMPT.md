# Android: Legal documents (Privacy + Terms)

Integrate StillFresh **Privacy Policy** and **Terms & Conditions** so customers and vendors can open them in-app and accept them at signup.

Legal text is published on the StillFresh marketing website. **Do not hardcode the full legal text in the Android app.** Always open the hosted URLs (or load them in an in-app browser).

---

## 1. Base URL

Use a build-config / remote-config constant:

| Environment | Example base URL |
|---|---|
| Local marketing site | `http://10.0.2.2:5174` (emulator) or your LAN IP `:5174` |
| Staging / production | `https://stillfresh.rs` *(replace with real domain when live)* |

Define something like:

```kotlin
object LegalUrls {
    // Prefer BuildConfig.MARKETING_BASE_URL
    const val BASE = BuildConfig.MARKETING_BASE_URL // no trailing slash

    val PRIVACY = "$BASE/privatnost"
    val TERMS_CUSTOMER = "$BASE/uslovi"
    val TERMS_VENDOR = "$BASE/uslovi-prodavci"
}
```

### Canonical paths

| Document | Path | Audience |
|---|---|---|
| Politika privatnosti | `/privatnost` | Everyone |
| Uslovi korišćenja (kupci) | `/uslovi` | Customer (`USER`) |
| Uslovi korišćenja (prodavci) | `/uslovi-prodavci` | Vendor (`VENDOR` / `VENDOR_ADMIN`) |

Optional aliases (also work): `/privacy`, `/terms`, `/terms/vendor`.

Repo source files (backend monorepo): `PRIVACY.md`, `TERMS_CUSTOMER.md`, `TERMS_VENDOR.md` — rendered by `marketing-web`.

---

## 2. How to open documents

**Prefer Chrome Custom Tabs** over a full-screen WebView for legal pages (better back/security UX). Fall back to WebView or external browser if Custom Tabs are unavailable.

```kotlin
fun openLegalUrl(context: Context, url: String) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
    customTabsIntent.launchUrl(context, Uri.parse(url))
}
```

Requirements:

- Enable JavaScript if you must use WebView (markdown site is static SPA).
- Use HTTPS in production.
- Handle offline: show a short message (“Proverite internet konekciju”) — do not ship stale bundled copies unless Product explicitly asks for offline cache.

---

## 3. Where to show links in the app

### 3.1 Signup / registration (required)

On **customer** and **vendor** registration screens:

1. Checkbox: *Prihvatam Uslove korišćenja i Politiku privatnosti*
2. Make **Uslove korišćenja** and **Politiku privatnosti** tappable spans that open the correct URLs:
   - Customer signup → `TERMS_CUSTOMER` + `PRIVACY`
   - Vendor apply / register → `TERMS_VENDOR` + `PRIVACY`
3. Disable Continue / Register until the checkbox is checked.
4. Do **not** require reading the full document; linking + explicit accept is enough for UX (legal review may refine this).

### 3.2 Settings / Profile (always available)

Add a **Pravni dokumenti** / **Legal** section:

| Role | Links |
|---|---|
| Customer | Politika privatnosti, Uslovi korišćenja |
| Vendor | Politika privatnosti, Uslovi za prodavce |
| Both / unclear | Show all three |

Suggested entry points: Profile → About / Legal, or Settings → Legal.

### 3.3 Store listings

Use the same public URLs in Google Play / App Store **Privacy Policy** fields:

- Privacy: `{BASE}/privatnost`

---

## 4. Role-based terms selection

```kotlin
fun termsUrlForRole(role: String?): String = when (role) {
    "VENDOR", "VENDOR_ADMIN" -> LegalUrls.TERMS_VENDOR
    else -> LegalUrls.TERMS_CUSTOMER
}
```

Privacy is always `LegalUrls.PRIVACY`.

---

## 5. Acceptance tracking (implemented backend)

The backend now records legal acceptance. Send the **version** of each document the user accepted; the server stamps the acceptance **timestamp** automatically (it is not client-controlled).

### 5.1 Version constant

Keep a single constant for the currently published document version. Bump it whenever the legal text meaningfully changes. Use the "Poslednje ažuriranje" date from the documents (currently **`2026-07-23`**).

```kotlin
object LegalDocs {
    const val VERSION = "2026-07-23" // must match the published documents
}
```

### 5.2 Customer registration — `POST /users/register`

Add the fields to the existing registration JSON body (the endpoint accepts the user object):

```json
{
  "username": "ana",
  "email": "ana@example.com",
  "password": "••••••",
  "termsVersion": "2026-07-23",
  "privacyVersion": "2026-07-23"
}
```

- Send `termsVersion` only when the Terms checkbox was accepted.
- Send `privacyVersion` to record that the Privacy Policy was acknowledged.
- The server sets `termsAcceptedAt` / `privacyAcceptedAt` to the server time when the matching version is present.

### 5.3 Vendor registration

- **Self-register** `POST /vendors/register` — add `termsVersion` and `privacyVersion` to the vendor JSON body (same semantics as above).
- **Apply** `POST /vendors/apply` — the `PendingVendorRegistrationRequest` now accepts `termsVersion` and `privacyVersion`; include them in the apply payload.

### 5.4 Response / read-back

The stored fields (`termsAcceptedAt`, `termsVersion`, `privacyAcceptedAt`, `privacyVersion`) are part of the user/vendor profile and can be shown in Settings (e.g. "Uslovi prihvaćeni: 23.07.2026").

### 5.5 Client-side hygiene

1. Still require the checkbox before enabling Register.
2. Also cache locally (`DataStore`): `termsVersion`, `privacyVersion`, `acceptedAt` for offline display.
3. **Re-acceptance:** if `LegalDocs.VERSION` is newer than the version the user previously accepted (from profile), prompt them to review and accept again on next login, then re-send the new version via the profile update / re-accept flow.

### 5.6 Known gap — OAuth2 / Google signup

The Google signup path (`/users/create-oauth2`) does **not** yet carry acceptance fields. For now, when a user signs up with Google, show the checkbox in-app and record acceptance locally; a follow-up backend change is needed to persist it for OAuth2 users. Flag this to Product if Google signup must have server-side acceptance from day one.

---

## 6. UX copy (Serbian)

Checkbox example:

> Prihvatam [Uslove korišćenja](terms) i [Politiku privatnosti](privacy).

Settings rows:

- Politika privatnosti  
- Uslovi korišćenja  
- Uslovi za prodavce *(vendor app / vendor mode only)*

Error if unchecked:

> Morate prihvatiti Uslove korišćenja i Politiku privatnosti da biste nastavili.

---

## 7. QA checklist

- [ ] Customer signup: both links open correct pages; register blocked until accept
- [ ] Vendor signup: vendor terms + privacy links work
- [ ] Profile/Settings legal links work for each role
- [ ] Back from Custom Tab returns to the same screen
- [ ] Works on emulator with local `marketing-web` (`npm run dev` on port **5174**)
- [ ] Production build uses HTTPS marketing base URL (not localhost)
- [ ] Play Console privacy URL matches `{BASE}/privatnost`

---

## 8. Local testing with marketing-web

From the StillFresh monorepo:

```bash
cd marketing-web
npm install
npm run dev
```

Site: **http://localhost:5174**

- Emulator → `http://10.0.2.2:5174/...`
- Physical device → `http://<your-pc-lan-ip>:5174/...` (same Wi‑Fi; cleartext may need network security config for HTTP)

Docker:

```bash
docker compose up -d --build marketing-web
```

---

## 9. Out of scope / do not do

- Do not paste the full markdown into Android string resources.
- Do not open vendor terms for pure customer accounts (or vice versa), except in a combined “all legal docs” list if desired.
- Do not block cancel/order flows on legal acceptance beyond signup (unless Product asks).
