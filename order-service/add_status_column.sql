-- Migration script to add status column to orders table
-- This column stores the order status for vendor confirmation/refusal workflow

-- Step 1: Add status column as nullable first (to avoid NOT NULL constraint on existing data)
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS status VARCHAR(50);

-- Step 2: Add updated_at column
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

-- Step 3: Update existing orders to have CONFIRMED status (since they were already finalized)
UPDATE orders SET status = 'CONFIRMED' WHERE status IS NULL;

-- Step 4: Set default value for future inserts
ALTER TABLE orders 
ALTER COLUMN status SET DEFAULT 'PENDING';

-- Step 5: Now make it NOT NULL (since all existing rows have values)
ALTER TABLE orders 
ALTER COLUMN status SET NOT NULL;

-- Step 6: Add constraint to ensure valid status values
ALTER TABLE orders 
ADD CONSTRAINT check_order_status 
CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'READY', 'COMPLETED', 'CANCELLED'));

COMMENT ON COLUMN orders.status IS 'Order status: PENDING (awaiting vendor confirmation), CONFIRMED (vendor confirmed), PROCESSING (vendor preparing), READY (ready for pickup), COMPLETED (order completed), CANCELLED (order cancelled)';
COMMENT ON COLUMN orders.updated_at IS 'Timestamp when order was last updated';

