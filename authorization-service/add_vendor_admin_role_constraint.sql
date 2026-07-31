-- Migration script to add VENDOR_ADMIN role to the users_role_check constraint
-- This allows VENDOR_ADMIN role to be stored in the users table

-- Drop the existing constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

-- Add the constraint with VENDOR_ADMIN included
ALTER TABLE users ADD CONSTRAINT users_role_check 
    CHECK (role::text = ANY (ARRAY[
        'USER'::character varying, 
        'ADMIN'::character varying, 
        'SUPER_ADMIN'::character varying, 
        'VENDOR'::character varying,
        'VENDOR_ADMIN'::character varying
    ]::text[]));

