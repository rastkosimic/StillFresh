-- Migration script to add VENDOR_ADMIN role to the vendor_role_check constraint
-- This allows VENDOR_ADMIN role to be stored in the vendor table

-- Connect to the vendor database first:
-- psql -h localhost -p 5432 -U stillfreshvendor -d stillfresh_vendordb

-- Drop the existing constraint
ALTER TABLE vendor DROP CONSTRAINT IF EXISTS vendor_role_check;

-- Add the constraint with VENDOR_ADMIN included
ALTER TABLE vendor ADD CONSTRAINT vendor_role_check 
    CHECK (role::text = ANY (ARRAY[
        'USER'::character varying, 
        'ADMIN'::character varying, 
        'SUPER_ADMIN'::character varying, 
        'VENDOR'::character varying,
        'VENDOR_ADMIN'::character varying
    ]::text[]));

-- Verify the constraint was updated
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conname = 'vendor_role_check';

