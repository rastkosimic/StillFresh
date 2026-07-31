-- AllSecure order-time 3DS: expose redirect URL and status for client polling.
ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS redirect_url VARCHAR(2048),
    ADD COLUMN IF NOT EXISTS authorization_status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(512);
