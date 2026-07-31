-- Migration script to add pickup_date column to offers table (Option B: date + times)
-- pickup_date is used to derive UI groupings like "Collect today" / "Collect tomorrow".

-- Step 1: Add column as nullable first (compatibility)
ALTER TABLE offers
ADD COLUMN IF NOT EXISTS pickup_date DATE;

-- Step 2: Backfill existing rows (best-effort)
-- If pickup_date is missing, default to CURRENT_DATE so existing rows remain usable.
UPDATE offers
SET pickup_date = CURRENT_DATE
WHERE pickup_date IS NULL;

-- Step 3: Add comment
COMMENT ON COLUMN offers.pickup_date IS 'Pickup date selected by vendor (Option B: date + pickupStartTime/pickupEndTime). Used for pickup time-slot grouping.';

-- Note: Keep nullable for gradual migration. After all clients send pickup_date, you can enforce NOT NULL:
-- ALTER TABLE offers ALTER COLUMN pickup_date SET NOT NULL;


