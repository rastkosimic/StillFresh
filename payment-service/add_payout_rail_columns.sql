-- Adds async-rail tracking and domestic bank detail columns to vendor_payout_items.
-- Required for the CMIplus payout integration (SUBMITTED state, pain.002 polling,
-- Serbian domestic account details).
-- Run manually against the payment-service database (ddl-auto: update also creates
-- these in development).

ALTER TABLE vendor_payout_items ADD COLUMN IF NOT EXISTS target_account_number VARCHAR(34);
ALTER TABLE vendor_payout_items ADD COLUMN IF NOT EXISTS target_bank_code VARCHAR(16);
ALTER TABLE vendor_payout_items ADD COLUMN IF NOT EXISTS bank_message_id VARCHAR(64);
ALTER TABLE vendor_payout_items ADD COLUMN IF NOT EXISTS rail_type VARCHAR(24);
ALTER TABLE vendor_payout_items ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;

-- Widen status columns is not needed (existing length 24 fits new enum values).

CREATE INDEX IF NOT EXISTS idx_vpi_bank_message_id ON vendor_payout_items (bank_message_id);
