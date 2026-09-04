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

## Production setup

Production uses the same variable names but a separate, gitignored `.env.prod` and an overlay compose file that keeps every service off the host network.

1. On the server, copy the production catalog and fill in real values:

   ```bash
   cp .env.prod.example .env.prod
   chmod 600 .env.prod
   ```

2. Generate a strong value for each secret. `JWT_SECRET` and `GATEWAY_INTERNAL_SECRET` must be at least 32 characters and identical across services:

   ```bash
   openssl rand -base64 48
   ```

3. Generate the Postgres init SQL from the production `.env.prod`, then start the stack:

   ```bash
   bash ./scripts/generate-init-sql.sh .env.prod
   docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build
   ```

The overlay sets `SPRING_PROFILES_ACTIVE=prod`, unpublishes all service and infrastructure ports, and exposes only the TLS reverse proxy on 80/443.

### Fail-closed configuration

The `prod` Spring profiles declare secrets with no fallback (`${JWT_SECRET}` rather than `${JWT_SECRET:}`), so a missing variable fails placeholder resolution and the service refuses to start. `ProdConfigGuard` in `shared-entities` adds the checks a missing-value test cannot make, rejecting startup when:

- `jwt.secret` or `gateway.internal.secret` is shorter than 32 characters, or still equals a published development default
- `spring.jpa.hibernate.ddl-auto` is anything other than `validate` or `none`
- `spring.jpa.show-sql` or `hibernate.format_sql` is true (SQL logging exposes personal data)
- `allsecure.base-url` points at `paymentsandbox.cloud`, uses plain HTTP, or the API key/username/integration key is still an AllSecure simulator credential
- `payout.rail` is `stub`, or `payout.cmiplus.stub-mode` is true, while `payout.auto.execute` is true — the stub rail marks vendor payouts complete without transferring funds

To run production with the bank rail not yet live, keep `PAYOUT_AUTO_EXECUTE=false`. Batches are then built and approved but nothing is disbursed.

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

## Rotate before going to production

These values are recoverable from committed history and must be treated as public. Removing them from `HEAD` did not make them secret; anyone with a clone can read them.

| Value | Where it leaked | Action |
|---|---|---|
| Stripe test secret key `sk_test_51QmG1a…` | `docker-compose.yml` in commit `2a3fe22`, removed in `eaa72d7` | Revoke in the Stripe dashboard and issue a new key |
| Seven DB passwords `FreshStill011*` | `docker-compose.yml` in commit `6120d9b`, externalized in `7aaa2ee` | New password per database; never reuse the pattern |
| `GATEWAY_INTERNAL_SECRET` dev default | Committed in every service's YAML and `.env.example` | New random value ≥32 chars for any shared environment |
| Bootstrap `SUPER_ADMIN` bcrypt hash | `db-init/templates/init-auth.sql.template` | Seed removed; provision the first admin manually |

Also rotate, since they were previously committed in YAML/SQL: Google OAuth client secret, Mailgun API key, Google Maps API key, and `JWT_SECRET` (rotating it invalidates all existing tokens).

Verify what is still reachable in history with:

```bash
git log --all --oneline -S "sk_test_51QmG1a"
git log --all --oneline -S "FreshStill011User"
```

Rotation is sufficient to close the exposure. Rewriting history with `git filter-repo` additionally removes the values from the repository, but requires every clone to be re-cloned and does not undo any exposure that already happened.
