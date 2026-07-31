# CMIplus Vendor Payout Integration

StillFresh pays MoR / AllSecure vendors via the **ledger payout pipeline** in `payment-service`. The default production rail is **Raiffeisen CMIplus Open APIs** (pain.001 submission, pain.002 status, CAMT reconciliation). C2B customer payments remain on AllSecure and are unchanged.

## Architecture

```
Daily scheduler → PayoutBatch (PENDING)
       ↓ auto (unless paused)
   APPROVED → IN_PROGRESS
       ↓ PayoutRail.submit() per vendor item
   SUBMITTED (async) or COMPLETED (sync stub)
       ↓ status poller (pain.002)
   COMPLETED / FAILED (+ ledger reversal on reject)
```

| Component | Role |
|-----------|------|
| `PayoutSchedulerService` | Creates batches from unsettled ledger credits |
| `PayoutAutoOrchestrator` | Auto-approves and submits batches |
| `PayoutSubmissionService` | Settles ledger at submit time, calls `PayoutRail` |
| `PayoutStatusPollingService` | Polls `SUBMITTED` items every 15 min |
| `CmiplusPayoutRail` | CMIplus pain.001 / pain.002 integration |
| `CamtReconciliationService` | Cross-checks payouts vs CAMT statements |

### Swappable rails (`payout.rail`)

| Value | Behaviour |
|-------|-----------|
| `stub` | Dev/test; immediate COMPLETED (default) |
| `sepa-xml` | Writes pain.001 files for manual bank upload |
| `cmiplus` | RBI CMIplus Open APIs (stub-mode until credentials ready) |

A future Serbia-specific domestic rail plugs in as another `PayoutRail` + `Pain001MessageBuilder` implementation.

## Phase 0 — Bank onboarding (your action)

Complete these steps with Raiffeisen Bank Serbia / RBI before disabling stub mode:

1. Open a **corporate RSD account** for StillFresh platform operations.
2. Register at [api.rbinternational.com](https://api.rbinternational.com) — create a non-private organisation and **save the Client Secret**.
3. Add the **CMIplus Core Cash Management API Bundle** (Payment Initiation, Payment Status, Account Statement).
4. Sign the **Corporate Seal** contract and obtain a CA-issued client certificate (SHA-256 TLS / mTLS).
5. Provide RBI with your **ClientID** and certificate public key + trust chain.
6. Test in the **sandbox** environment from the marketplace.
7. (Recommended) Enable **On-Time CAMT052 notifications** on the platform account.

Contact: susanne.prager@rbinternational.com or your Raiffeisen Serbia relationship manager.

Deliverables for engineering: sandbox ClientID/Secret, keystore file, debtor IBAN, and exported OpenAPI paths from the marketplace.

## Configuration

Set these environment variables (see `payment-service/src/main/resources/application.yml`):

```bash
# Rail selection
PAYOUT_RAIL=cmiplus                    # stub | sepa-xml | cmiplus

# Automatic pipeline (default ON)
PAYOUT_AUTO_ENABLED=true
PAYOUT_AUTO_APPROVE=true
PAYOUT_AUTO_EXECUTE=true

# Platform debtor account (StillFresh corporate account)
PLATFORM_IBAN=RS...
PLATFORM_ACCOUNT_HOLDER=StillFresh d.o.o.
PLATFORM_BANK_NAME=Raiffeisen banka

# CMIplus — keep stub until onboarding complete
CMIPLUS_STUB_MODE=true
CMIPLUS_BASE_URL=                      # from marketplace after registration
CMIPLUS_CLIENT_ID=
CMIPLUS_CLIENT_SECRET=
CMIPLUS_KEYSTORE_PATH=
CMIPLUS_KEYSTORE_PASSWORD=
CMIPLUS_STUB_COMPLETE_DELAY_SECONDS=30

# Optional: override API paths once OpenAPI spec is available
CMIPLUS_TOKEN_PATH=/oauth/token
CMIPLUS_PAYMENT_INITIATION_PATH=/payment-initiation
CMIPLUS_PAYMENT_STATUS_PATH=/payment-status
CMIPLUS_ACCOUNT_STATEMENT_PATH=/account-statement
```

When sandbox credentials are ready, set `CMIPLUS_STUB_MODE=false` and fill in base URL + credentials.

## Admin API (`/ledger/payouts/*`)

All endpoints require `ROLE_ADMIN` or `ROLE_SUPER_ADMIN` via API gateway.

| Method | Path | Action |
|--------|------|--------|
| `GET` | `/ledger/payouts` | List batches |
| `GET` | `/ledger/payouts/{id}` | Batch + items |
| `POST` | `/ledger/payouts/run` | Trigger scheduler manually |
| `GET` | `/ledger/payouts/{id}/dry-run` | Preview transfers |
| `POST` | `/ledger/payouts/{id}/approve` | Approve batch |
| `POST` | `/ledger/payouts/{id}/execute` | Submit to rail |
| `POST` | `/ledger/payouts/{id}/retry-failed` | Retry FAILED items |
| `POST` | `/ledger/payouts/auto/pause` | Pause automatic pipeline |
| `POST` | `/ledger/payouts/auto/resume` | Resume automatic pipeline |
| `GET` | `/ledger/payouts/auto/status` | Check auto-enabled flag |
| `POST` | `/ledger/payouts/{id}/hold` | Hold batch (blocks auto) |
| `POST` | `/ledger/payouts/{id}/release` | Release hold → re-enters auto |
| `POST` | `/ledger/payouts/{id}/cancel` | Cancel unreleased batch |
| `POST` | `/ledger/payouts/{batchId}/items/{itemId}/hold` | Hold single item |
| `GET` | `/ledger/payouts/reconciliation/report` | CAMT reconciliation report |

## Item status lifecycle

```
SCHEDULED → SUBMITTED → COMPLETED
                     → FAILED (ledger reversed)
```

Ledger credits are **settled at submit time**, not at batch creation. If the bank rejects a transfer after submission, a `PAYOUT_REVERSAL` entry restores the vendor balance.

## Vendor bank details

Payout items snapshot vendor bank data at scheduling time:

- IBAN **or** domestic account number + bank code (BIC/SWIFT)
- Stripe Connect vendors are **excluded** from ledger batches

## Database migration

Run manually if not using `ddl-auto: update`:

```bash
psql -f payment-service/add_payout_rail_columns.sql
```

## Local development

```bash
# Default: stub rail, auto pipeline ON
PAYOUT_RAIL=stub docker-compose up -d payment-service

# Test CMIplus stub (SUBMITTED → COMPLETED after 30s)
PAYOUT_RAIL=cmiplus CMIPLUS_STUB_MODE=true docker-compose up -d --build payment-service
```

## Failure recovery

| Situation | Action |
|-----------|--------|
| Batch stuck IN_PROGRESS with SUBMITTED items | Wait for poller or check bank pain.002 manually |
| Item FAILED after bank reject | Credits restored automatically; fix bank details, retry batch |
| Need to stop all payouts | `POST /ledger/payouts/auto/pause` |
| Need to review before pay | `POST /ledger/payouts/{id}/hold` before auto runs, or pause auto globally |
| Batch submitted by mistake | Cannot cancel once SUBMITTED; contact bank |

## MoR legacy path

Manual `requestManualPayout()` in vendor-service is **deprecated**. All MoR vendor payouts flow through the ledger pipeline. Historical MoR admin endpoints (`/admin/mor/**`) remain for read-only audit.
