-- Migration script to add payment_intent_id column to orders table
-- This column stores the Stripe PaymentIntent ID for manual capture/cancel flow (Too Good To Go style)

ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(255);

COMMENT ON COLUMN orders.payment_intent_id IS 'Stripe PaymentIntent ID for manual capture/cancel flow. Used in Too Good To Go style payment where payment is authorized at order placement and captured at pickup.';

