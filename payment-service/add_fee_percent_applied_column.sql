-- Platform fee percentage frozen at payment initiation time (for audit when global rate changes).
ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS fee_percent_applied DECIMAL(5, 2);

ALTER TABLE bank_transfer_payments
    ADD COLUMN IF NOT EXISTS fee_percent_applied DECIMAL(5, 2);
