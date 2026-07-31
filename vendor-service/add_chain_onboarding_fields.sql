-- Migration script: Add chain and onboarding support to vendor table
-- This script adds new columns and migrates existing vendor data

-- ========== Add New Columns ==========

-- Chain identification fields
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS chain_id VARCHAR(255);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS chain_name VARCHAR(255);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS location_name VARCHAR(255);
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS is_chain_location BOOLEAN DEFAULT FALSE;
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS is_headquarters BOOLEAN DEFAULT FALSE;
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS is_unique_vendor BOOLEAN DEFAULT TRUE;

-- Business registration
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS business_registration_id VARCHAR(255);

-- Onboarding status
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS onboarding_status VARCHAR(50);

-- Banking model fields
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS uses_shared_payment_account BOOLEAN DEFAULT FALSE;
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS shared_payment_account_vendor_id BIGINT;

-- Worker assignment (for VENDOR role workers)
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS assigned_location_id BIGINT;

-- ========== Create Indexes for Performance ==========

CREATE INDEX IF NOT EXISTS idx_vendor_chain_id ON vendor(chain_id);
CREATE INDEX IF NOT EXISTS idx_vendor_chain_name ON vendor(chain_name);
CREATE INDEX IF NOT EXISTS idx_vendor_onboarding_status ON vendor(onboarding_status);
CREATE INDEX IF NOT EXISTS idx_vendor_shared_payment ON vendor(shared_payment_account_vendor_id);
CREATE INDEX IF NOT EXISTS idx_vendor_assigned_location ON vendor(assigned_location_id);
CREATE INDEX IF NOT EXISTS idx_vendor_is_headquarters ON vendor(is_headquarters) WHERE is_headquarters = TRUE;

-- ========== Data Migration for Existing Vendors ==========

-- Mark all existing vendors as unique vendors (standalone)
-- Set their onboarding status to COMPLETED (they've already been using the system)
UPDATE vendor 
SET 
    is_unique_vendor = TRUE,
    is_chain_location = FALSE,
    is_headquarters = FALSE,
    onboarding_status = 'COMPLETED',
    uses_shared_payment_account = FALSE,
    shared_payment_account_vendor_id = NULL
WHERE onboarding_status IS NULL;

-- Set location_name to username for existing vendors (for display purposes)
UPDATE vendor 
SET location_name = username
WHERE location_name IS NULL AND username IS NOT NULL;

-- ========== Add Foreign Key Constraint ==========

-- Foreign key for shared payment account (self-referencing)
ALTER TABLE vendor 
ADD CONSTRAINT fk_vendor_shared_payment_account 
FOREIGN KEY (shared_payment_account_vendor_id) 
REFERENCES vendor(id)
ON DELETE SET NULL;

-- Foreign key for assigned location (for workers)
ALTER TABLE vendor 
ADD CONSTRAINT fk_vendor_assigned_location 
FOREIGN KEY (assigned_location_id) 
REFERENCES vendor(id)
ON DELETE SET NULL;

-- ========== Add Check Constraints ==========

-- Ensure headquarters is only set for chain locations
ALTER TABLE vendor 
ADD CONSTRAINT chk_headquarters_chain 
CHECK (
    (is_headquarters = TRUE AND is_chain_location = TRUE) 
    OR is_headquarters = FALSE 
    OR is_headquarters IS NULL
);

-- Ensure shared payment account vendor exists if usesSharedPaymentAccount is true
ALTER TABLE vendor 
ADD CONSTRAINT chk_shared_payment_account 
CHECK (
    (uses_shared_payment_account = TRUE AND shared_payment_account_vendor_id IS NOT NULL)
    OR uses_shared_payment_account = FALSE
    OR uses_shared_payment_account IS NULL
);

-- Ensure chain_id is set if is_chain_location is true
ALTER TABLE vendor 
ADD CONSTRAINT chk_chain_id 
CHECK (
    (is_chain_location = TRUE AND chain_id IS NOT NULL)
    OR is_chain_location = FALSE
    OR is_chain_location IS NULL
);

-- ========== Comments for Documentation ==========

COMMENT ON COLUMN vendor.chain_id IS 'Unique identifier for the chain (UUID). All locations in the same chain share this ID.';
COMMENT ON COLUMN vendor.chain_name IS 'Brand/chain name (e.g., "McDonald''s"). Null for unique vendors.';
COMMENT ON COLUMN vendor.location_name IS 'Location identifier within the chain (e.g., "Downtown", "Airport").';
COMMENT ON COLUMN vendor.is_chain_location IS 'True if this vendor is part of a chain with multiple locations.';
COMMENT ON COLUMN vendor.is_headquarters IS 'True if this is the headquarters location for a chain (selling location, not corporate office).';
COMMENT ON COLUMN vendor.is_unique_vendor IS 'True if this is a standalone vendor (can upgrade to chain later).';
COMMENT ON COLUMN vendor.business_registration_id IS 'Business registration or tax ID number used for verification.';
COMMENT ON COLUMN vendor.onboarding_status IS 'Current onboarding status: PENDING_VERIFICATION, VERIFIED, TYPE_SELECTED, HEADQUARTERS_ADDED, BANKING_SETUP, PAYMENT_CONFIGURED, COMPLETED.';
COMMENT ON COLUMN vendor.uses_shared_payment_account IS 'True if this location uses a shared payment account (chain model).';
COMMENT ON COLUMN vendor.shared_payment_account_vendor_id IS 'Points to the vendor ID that owns the shared payment account (typically headquarters).';
COMMENT ON COLUMN vendor.assigned_location_id IS 'For VENDOR workers: Links worker to their assigned location. Null for VENDOR_ADMIN accounts.';

