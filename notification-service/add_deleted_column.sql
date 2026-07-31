-- Add deleted column to notifications table (soft delete for user "delete" in app)
-- This script can be run manually if not using DatabaseInitializer

-- Connect to the notification database first:
-- psql -h <host> -p 5432 -U stillfreshnotification -d stillfresh_notificationdb

-- Add the deleted column
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for listing by user excluding deleted
CREATE INDEX IF NOT EXISTS idx_notifications_user_deleted ON notifications(user_id, deleted);

-- Verify the column was added
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'notifications' AND column_name = 'deleted';
