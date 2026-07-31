-- Migration script to add newPasswordHash column to password_reset_token table
-- This column stores the encoded new password temporarily until email verification

ALTER TABLE password_reset_token 
ADD COLUMN IF NOT EXISTS new_password_hash VARCHAR(255);

-- Add comment for documentation
COMMENT ON COLUMN password_reset_token.new_password_hash IS 'Stores the encoded new password hash temporarily until email verification is completed';

