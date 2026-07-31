# StillFresh Marketing Website

Public company / marketing site for StillFresh. For now it mainly hosts legal documents; marketing pages can be added later.

> **Not a Maven module.** Use **npm** (local) or **Docker**.

## Legal pages (source of truth)

Markdown at the **repo root** is imported at build time:

| Document | Source file | URL path |
|---|---|---|
| Privacy policy | `PRIVACY.md` | `/privatnost` |
| Customer terms | `TERMS_CUSTOMER.md` | `/uslovi` |
| Vendor terms | `TERMS_VENDOR.md` | `/uslovi-prodavci` |

English aliases redirect: `/privacy` → `/privatnost`, `/terms` → `/uslovi`, `/terms/vendor` → `/uslovi-prodavci`.

## Local development

```bash
cd marketing-web
npm install
npm run dev
```

Open **http://localhost:5174**

## Docker

```bash
# from repo root
docker compose up -d --build marketing-web
```

Open **http://localhost:5174**

## Production URL

Set the public base URL when you deploy (examples):

- `https://stillfresh.rs`
- `https://www.stillfresh.rs`

Android should use that base + the paths above. See [`ANDROID_LEGAL_DOCUMENTS_INTEGRATION_PROMPT.md`](../ANDROID_LEGAL_DOCUMENTS_INTEGRATION_PROMPT.md).

## Brand

- Green `#2E5A27`
- Cream `#F5F0E6`
- Orange `#E8671A`
