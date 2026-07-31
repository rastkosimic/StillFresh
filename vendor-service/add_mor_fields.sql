-- Migration: Replace Payoneer with MoR (Merchant of Record) model
-- Date: 2024

-- Remove Payoneer field
ALTER TABLE vendor DROP COLUMN IF EXISTS payoneer_account_id;

-- Update payment_provider enum values (if using enum type, otherwise just update data)
-- For VARCHAR: Update existing PAYONEER to MOR
UPDATE vendor SET payment_provider = 'MOR' WHERE payment_provider = 'PAYONEER';

-- Add MoR-specific fields
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS payout_model VARCHAR(20);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS balance DECIMAL(12,2) DEFAULT 0;
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS manual_payout_method VARCHAR(20);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS bank_account_holder_name VARCHAR(255);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(100);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS bank_swift_code VARCHAR(20);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS bank_iban VARCHAR(50);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_vendor_payout_model ON vendor(payout_model);
CREATE INDEX IF NOT EXISTS idx_vendor_balance ON vendor(balance) WHERE balance > 0;

-- Create vendor_balance_transactions table
CREATE TABLE IF NOT EXISTS vendor_balance_transactions (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    order_id BIGINT,
    payout_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vendor_balance_transactions_vendor_id ON vendor_balance_transactions(vendor_id);
CREATE INDEX IF NOT EXISTS idx_vendor_balance_transactions_created_at ON vendor_balance_transactions(created_at DESC);

-- Create vendor_payouts table
CREATE TABLE IF NOT EXISTS vendor_payouts (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    transaction_reference VARCHAR(255),
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_vendor_payouts_vendor_id ON vendor_payouts(vendor_id);
CREATE INDEX IF NOT EXISTS idx_vendor_payouts_status ON vendor_payouts(status);
CREATE INDEX IF NOT EXISTS idx_vendor_payouts_requested_at ON vendor_payouts(requested_at DESC);

-- Update comments
COMMENT ON COLUMN vendor.payout_model IS 'Payout model: CONNECT (Stripe Connect) or MOR (Merchant of Record)';
COMMENT ON COLUMN vendor.balance IS 'Internal balance for MoR vendors (in cents)';
COMMENT ON COLUMN vendor.manual_payout_method IS 'Manual payout method for MoR vendors: BANK, WISE, etc.';
COMMENT ON COLUMN vendor.bank_account_holder_name IS 'Bank account holder name for MoR vendors';
COMMENT ON COLUMN vendor.bank_account_number IS 'Bank account number for MoR vendors';
COMMENT ON COLUMN vendor.bank_name IS 'Bank name for MoR vendors';
COMMENT ON COLUMN vendor.bank_swift_code IS 'Bank SWIFT/BIC code for MoR vendors';
COMMENT ON COLUMN vendor.bank_iban IS 'Bank IBAN for MoR vendors (if applicable)';

COMMENT ON TABLE vendor_balance_transactions IS 'Transaction history for MoR vendor balances';
COMMENT ON TABLE vendor_payouts IS 'Manual payout requests for MoR vendors';

