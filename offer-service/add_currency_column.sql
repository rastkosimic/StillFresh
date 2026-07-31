-- Migration script to add currency column to offers table
-- This column stores the ISO currency code (e.g., "EUR", "RSD", "USD") 
-- determined based on the offer's geographic location

-- Step 1: Add column as nullable first (for compatibility)
ALTER TABLE offers 
ADD COLUMN IF NOT EXISTS currency VARCHAR(3);

-- Step 2: Set default value for existing rows
UPDATE offers 
SET currency = 'EUR' 
WHERE currency IS NULL;

-- Step 3: Update existing offers in Serbia region to RSD (best-effort)
UPDATE offers 
SET currency = 'RSD' 
WHERE currency = 'EUR' 
  AND latitude BETWEEN 42.0 AND 47.0 
  AND longitude BETWEEN 18.0 AND 23.0;

-- Step 4: Make column NOT NULL with default (after setting values)
ALTER TABLE offers 
ALTER COLUMN currency SET NOT NULL;

ALTER TABLE offers 
ALTER COLUMN currency SET DEFAULT 'EUR';

-- Add comment
COMMENT ON COLUMN offers.currency IS 'ISO currency code determined from offer location. Used for pricing display and payment processing.';

