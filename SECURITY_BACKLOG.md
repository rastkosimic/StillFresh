# Security backlog (follow-up)

Items deferred from the production-hardening pass. Tackle these after the `dev`/`prod` profiles and CRITICAL/HIGH fixes are deployed.

## Authentication and abuse prevention

- Rate limiting on `/auth/login`, `/auth/forgot-password`, `/auth/refresh-token`, and OAuth login (Redis / Bucket4j / WAF).
- Account enumeration: return generic responses on forgot-password and check-availability endpoints.
- Reject refresh tokens used as Bearer tokens in `authorization-service` `JwtRequestFilter` (mirror gateway behaviour).
- Validate Google OAuth `aud` against `GOOGLE_CLIENT_ID` instead of deprecated tokeninfo-only checks.
- Strengthen password policy (length ≥ 12, breach check) and BCrypt cost factor ≥ 12.
- Blacklist JWTs by `jti` only; remove full-token Redis keys.

## Business logic and integrity

- Atomic offer quantity decrement (`UPDATE … WHERE qty >= ?` or `@Version`) to prevent oversell races.
- Pickup verification codes: high-entropy, single-use codes required for vendor confirm-pickup.
- AllSecure callback replay window on `Date` header; idempotent handler HTTP status semantics.
- Compare AllSecure callback amount against stored `PaymentTransaction` before settlement.
- Stripe webhook endpoint with signature verification for async events.
- ShedLock (or equivalent) for schedulers running on every replica (order expiry, offer invalidation, notification retention).

## Data minimization

- Trim PII from Kafka events (`OrderPlacedEvent`, `OrderRequestEvent`, etc.) — publish IDs only.
- GDPR anonymization on account deletion (user-service / vendor-service soft-delete leaves PII).
- Stop returning temporary passwords in API responses and plaintext credential emails; use one-time setup links.

## Application hardening

- Upload magic-byte validation on vendor image upload.
- `POST /payment/charge` server-side amount validation or removal.
- Payment amount revalidation from offer/order before Kafka-driven preauthorize.
- Disable Swagger/springdoc in all environments except explicit dev profile (partially done for prod).
- Restrict notification-service `/actuator/**` in production.
- Eureka server authentication or replace with managed service discovery.
- Kafka SASL/TLS and topic ACLs on the production broker.
- Pin Docker base image digests in CI.
- `npm audit fix` and Vite major upgrade for `admin-web` and `marketing-web`.
- Consider httpOnly cookies for admin JWT storage instead of `sessionStorage`.

## Operational

- Rotate secrets recoverable from git history (see [SECRETS.md](SECRETS.md)); optional `git filter-repo` to purge history.
- Run OWASP Dependency-Check on Maven and npm lockfiles periodically.
