-- Add stripe_account_id column to vendor table
-- This script should be run manually on the vendor database for existing installations
-- For new installations, Hibernate will automatically create the column (ddl-auto: update)

-- Connect to the vendor database first:
-- psql -h localhost -p 5432 -U stillfreshvendor -d stillfresh_vendordb

-- Add the stripe_account_id column (nullable, as existing vendors won't have this)
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS stripe_account_id VARCHAR(255);

-- Create index for better performance when looking up vendors by Stripe account ID
CREATE INDEX IF NOT EXISTS idx_vendor_stripe_account_id ON vendor(stripe_account_id);

-- Verify the column was added
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'vendor' AND column_name = 'stripe_account_id';


