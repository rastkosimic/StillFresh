-- Add default_payment_method_id to payment_users for caching Stripe default (avoids Customer.retrieve when listing payment methods)
ALTER TABLE payment_users ADD COLUMN IF NOT EXISTS default_payment_method_id VARCHAR(255);
