-- Add is_read column to notifications table
-- This script should be run manually on the notification database

-- Connect to the notification database first:
-- psql -h localhost -p 5432 -U stillfreshnotification -d stillfresh_notificationdb

-- Add the is_read column
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

-- Verify the column was added
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'notifications' AND column_name = 'is_read';
