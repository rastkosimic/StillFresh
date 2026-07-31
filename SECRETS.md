# Secrets management (StillFresh)

All sensitive values (passwords, API keys, signing secrets, OAuth client secrets) are supplied via **environment variables**. Locally they live in a gitignored root `.env`. Docker Compose injects them into each service. Production can inject the **same variable names** from Docker secrets, Kubernetes Secrets, or a cloud secrets manager—no YAML changes required.

## Local setup

1. Copy the catalog and fill real values:

   ```bash
   cp .env.example .env
   ```

2. Generate Postgres init SQL from templates (substitutes `__POSTGRES_PASSWORD_*__` from `.env`):

   ```powershell
   powershell -File .\scripts\generate-init-sql.ps1
   ```

   ```bash
   bash ./scripts/generate-init-sql.sh
   ```

3. Start the stack:

   ```bash
   docker-compose up -d --build
   ```

**Never commit `.env` or generated `init-*.sql`.** Commit only `.env.example` and `db-init/templates/*.sql.template`.

### Existing database volumes

Postgres init scripts run only on first volume creation. If you change DB passwords in `.env`, regenerate init SQL **and** recreate volumes (`docker-compose down -v`) or `ALTER USER` manually inside each database.

## Variable catalog

| Variable | Used by |
|----------|---------|
| `POSTGRES_PASSWORD_*` | Matching `*-postgres` containers + app services |
| `JWT_SECRET` | api-gateway, authorization, user, vendor, payment, notification |
| `GATEWAY_INTERNAL_SECRET` | All services (gateway trust header) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | authorization-service |
| `MAILGUN_*` | authorization, user, vendor, notification |
| `STRIPE_API_KEY` | payment-service |
| `GOOGLE_MAPS_API_KEY` | vendor-service |
| `FIREBASE_CREDENTIALS_HOST_PATH` | docker-compose volume for notification-service |
| `ALLSECURE_*` / `CMIPLUS_*` / `PLATFORM_*` | payment-service |

Committed `application.yml` files must use `${VAR:}` (empty default) for secrets so missing values fail closed.

## Rotate after this externalization

These values were previously committed in YAML/SQL. Treat them as exposed and rotate:

1. Google OAuth client secret
2. Mailgun API key
3. Stripe secret key (even test keys)
4. Shared `JWT_SECRET` (invalidates all existing tokens)
5. Google Maps API key
6. Database passwords (`FreshStill011*`) for any shared/non-local environments
7. `GATEWAY_INTERNAL_SECRET` for any shared/staging/prod environment
