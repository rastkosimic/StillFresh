-- Migration script to add pickup deadline and reminder flag to orders table
-- Used for order expiry (mark EXPIRED after pickup window) and 1-hour pickup reminder

-- Step 1: Add pickup_by column (deadline by which order must be picked up)
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS pickup_by TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN orders.pickup_by IS 'Deadline by which the order must be picked up; after this the order can be marked EXPIRED.';

-- Step 2: Add pickup_reminder_sent flag (so we send the 1-hour reminder only once)
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS pickup_reminder_sent BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN orders.pickup_reminder_sent IS 'Whether the "pick up by [time]" reminder (e.g. 1 hour before) has been sent.';

-- Step 3: Allow EXPIRED in the status constraint (drop old constraint, add new one)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS check_order_status;
ALTER TABLE orders
ADD CONSTRAINT check_order_status
CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'READY', 'COMPLETED', 'CANCELLED', 'EXPIRED'));
