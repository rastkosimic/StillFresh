-- Add pickup deadline and reminder flag for order expiry and 1-hour reminder
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS pickup_by TIMESTAMP WITH TIME ZONE;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS pickup_reminder_sent BOOLEAN NOT NULL DEFAULT false;

-- Allow EXPIRED in status constraint
ALTER TABLE orders DROP CONSTRAINT IF EXISTS check_order_status;
ALTER TABLE orders
ADD CONSTRAINT check_order_status
CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'READY', 'COMPLETED', 'CANCELLED', 'EXPIRED'));
