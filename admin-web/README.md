# StillFresh Super Admin Dashboard

React + Vite admin SPA for platform operators (`ADMIN` / `SUPER_ADMIN`).

> **Not a Maven module.** This folder has no `pom.xml`. Use **npm** (local dev) or **Docker** (below). Run `mvn clean install` from the repo root for Java microservices only.

## Prerequisites

- Node.js 20+
- StillFresh backend running via Docker Compose (API gateway on **http://localhost:8080**)

## First-time SUPER_ADMIN bootstrap

If no super admin exists yet:

```bash
curl -X POST http://localhost:8080/admin/create-initial-admin \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","email":"admin@stillfresh.com","password":"YourSecurePassword123"}'
```

## Local development

```bash
cd admin-web
cp .env.example .env.development   # if needed
npm install
npm run dev
```

Open **http://localhost:5173** and sign in with admin credentials.

> **Docker vs local dev:** Both use port **5173**. Run either `npm run dev` **or** the Docker container — not both at once.

## Environment

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | API gateway base URL |

## Features

- **Dashboard** — KPIs: customers, vendors, pending approvals, bank transfers, platform fee, payout pipeline
- **Customers** — profiles + auth accounts, activate/deactivate, promote/demote, order history (SUPER_ADMIN)
- **Vendors** — list, pending approvals, verify/activate, dashboard stats, ledger balance
- **Orders** (SUPER_ADMIN) — paginated list, status updates, cancel, confirm pickup, delete
- **Payments** — platform fee, ledger batches, MoR payouts, bank transfer confirmation
- **Admin Users** (SUPER_ADMIN) — list admins, register new admin, delete ADMIN accounts

## Docker (production build)

```bash
docker compose up -d --build admin-web
```

Open **http://localhost:5173** (nginx serves the built SPA from the container).

## Build

```bash
npm run build
npm run preview
```

## Brand colors

- Green `#2E5A27` — sidebar, primary nav
- Cream `#F5F0E6` — page background
- Orange `#E8671A` — primary actions
