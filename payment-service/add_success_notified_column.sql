-- Add success_notified to payment_transactions (dedupes AllSecure sync result vs async callback).
-- DEFAULT false backfills existing rows so the NOT NULL constraint can be applied to a populated table.
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS success_notified boolean NOT NULL DEFAULT false;
