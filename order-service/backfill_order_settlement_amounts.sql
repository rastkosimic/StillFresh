-- Backfill settlement snapshot on COMPLETED orders from payment-service data.
-- Run against order-service DB after add_order_settlement_columns.sql.
-- Requires dblink or manual cross-DB join; this script uses payment_intent_id linkage
-- when payment_transactions live in a separate database, run the UPDATE in two steps
-- or use a federated query. Below assumes a one-time export/import or shared access.

-- Step 1: Backfill from payment_transactions (Stripe / AllSecure) via payment_intent_id.
-- Replace stillfresh_paymentdb connection details for your environment.
--
-- Example when both schemas are accessible (adjust database names):
--
-- UPDATE orders o
-- SET gross_amount_cents = pt.gross_amount_cents,
--     platform_fee_cents = pt.platform_fee_cents,
--     net_amount_cents = pt.net_amount_cents,
--     fee_percent_applied = pt.fee_percent_applied,
--     settled_at = COALESCE(o.settled_at, o.updated_at, o.created_at)
-- FROM stillfresh_paymentdb.public.payment_transactions pt
-- WHERE o.payment_intent_id = pt.payment_intent_id
--   AND o.status = 'COMPLETED'
--   AND o.net_amount_cents IS NULL;

-- Step 2: Infer fee_percent when missing but cents are present.
UPDATE orders
SET fee_percent_applied = ROUND((platform_fee_cents::numeric / NULLIF(gross_amount_cents, 0)) * 100, 2)
WHERE status = 'COMPLETED'
  AND net_amount_cents IS NOT NULL
  AND gross_amount_cents > 0
  AND fee_percent_applied IS NULL;

-- Step 3: For COMPLETED orders without snapshot, approximate from total_price at 10% default fee
-- (only when no payment record exists — legacy data fallback).
UPDATE orders
SET gross_amount_cents = ROUND(total_price * 100),
    platform_fee_cents = ROUND(total_price * 100 * 0.10),
    net_amount_cents = ROUND(total_price * 100 * 0.90),
    fee_percent_applied = 10.0,
    settled_at = COALESCE(settled_at, updated_at, created_at)
WHERE status = 'COMPLETED'
  AND net_amount_cents IS NULL
  AND total_price > 0;
