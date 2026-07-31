-- Migration: Change ratings from one-per-user-vendor to one-per-order
-- Run manually against the vendor-service database.

ALTER TABLE ratings DROP CONSTRAINT IF EXISTS uk_rating_user_vendor;

ALTER TABLE ratings ALTER COLUMN order_id SET NOT NULL;

ALTER TABLE ratings ADD CONSTRAINT uk_rating_order UNIQUE (order_id);
