-- Migration script to increase column sizes for notifications table
-- This fixes the "value too long for type character varying(255)" error

-- Alter title column to VARCHAR(500)
ALTER TABLE notifications 
ALTER COLUMN title TYPE VARCHAR(500);

-- Alter body column to TEXT (unlimited length)
ALTER TABLE notifications 
ALTER COLUMN body TYPE TEXT;

-- Alter message column to TEXT (unlimited length)
ALTER TABLE notifications 
ALTER COLUMN message TYPE TEXT;

-- Alter error column to TEXT (unlimited length)
ALTER TABLE notifications 
ALTER COLUMN error TYPE TEXT;

