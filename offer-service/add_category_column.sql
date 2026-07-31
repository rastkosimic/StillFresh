-- Migration script to add category column to offers table
-- This column stores the offer category enum value (e.g., "MEALS", "GROCERIES")
-- Categories are language-agnostic; translations are handled in the application layer

-- Step 1: Add column as nullable first (for compatibility with existing data)
ALTER TABLE offers 
ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Step 2: Set default category based on businessType for existing offers
-- This is a best-effort mapping to populate existing data
UPDATE offers 
SET category = CASE 
    WHEN LOWER(business_type) LIKE '%restaurant%' OR 
         LOWER(business_type) LIKE '%cafe%' OR 
         LOWER(business_type) LIKE '%bistro%' OR
         LOWER(business_type) LIKE '%food%' OR
         LOWER(business_type) LIKE '%meal%' THEN 'MEALS'
    WHEN LOWER(business_type) LIKE '%bakery%' OR 
         LOWER(business_type) LIKE '%bread%' OR 
         LOWER(business_type) LIKE '%pastry%' OR
         LOWER(business_type) LIKE '%patisserie%' THEN 'BREAD_PASTRIES'
    WHEN LOWER(business_type) LIKE '%florist%' OR 
         LOWER(business_type) LIKE '%flower%' OR 
         LOWER(business_type) LIKE '%plant%' THEN 'FLOWERS_PLANTS'
    WHEN LOWER(business_type) LIKE '%pet%' OR 
         LOWER(business_type) LIKE '%animal%' THEN 'PET_FOOD'
    ELSE 'GROCERIES'  -- Default fallback
END
WHERE category IS NULL;

-- Step 3: Add comment
COMMENT ON COLUMN offers.category IS 'Offer category enum value (MEALS, GROCERIES, etc.). Language-agnostic; translations provided via API.';

-- Note: Column remains nullable to allow gradual migration.
-- New offers should always have a category set.
-- After all existing offers are migrated, you can make it NOT NULL:
-- ALTER TABLE offers ALTER COLUMN category SET NOT NULL;

