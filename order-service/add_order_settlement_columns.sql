-- Financial snapshot frozen at payment settlement (for vendor earnings display and fee audit).
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS gross_amount_cents BIGINT,
    ADD COLUMN IF NOT EXISTS platform_fee_cents BIGINT,
    ADD COLUMN IF NOT EXISTS net_amount_cents BIGINT,
    ADD COLUMN IF NOT EXISTS fee_percent_applied DECIMAL(5, 2),
    ADD COLUMN IF NOT EXISTS settled_at TIMESTAMPTZ;
