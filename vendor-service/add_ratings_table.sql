-- Migration: Add ratings table for vendor ratings
-- Date: 2024
-- Description: Creates a ratings table to store user ratings for vendors.
--              Ratings include 4 categories: collection process, quality, quantity, and variety.
--              Each category is rated from 1 to 5 stars. The total rating is the average of these 4 categories.

-- Create ratings table
CREATE TABLE IF NOT EXISTS ratings (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    collection_process_rating INTEGER NOT NULL CHECK (collection_process_rating >= 1 AND collection_process_rating <= 5),
    quality_rating INTEGER NOT NULL CHECK (quality_rating >= 1 AND quality_rating <= 5),
    quantity_rating INTEGER NOT NULL CHECK (quantity_rating >= 1 AND quantity_rating <= 5),
    variety_rating INTEGER NOT NULL CHECK (variety_rating >= 1 AND variety_rating <= 5),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_rating_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id) ON DELETE CASCADE,
    CONSTRAINT uk_rating_order UNIQUE (order_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_rating_vendor_id ON ratings(vendor_id);
CREATE INDEX IF NOT EXISTS idx_rating_user_id ON ratings(user_id);
CREATE INDEX IF NOT EXISTS idx_rating_order_id ON ratings(order_id);

-- Add comment to table
COMMENT ON TABLE ratings IS 'Stores user ratings for vendors with 4 categories: collection process, quality, quantity, and variety';
COMMENT ON COLUMN ratings.collection_process_rating IS 'Rating for collection process (1-5 stars)';
COMMENT ON COLUMN ratings.quality_rating IS 'Rating for food quality (1-5 stars)';
COMMENT ON COLUMN ratings.quantity_rating IS 'Rating for food quantity (1-5 stars)';
COMMENT ON COLUMN ratings.variety_rating IS 'Rating for food variety (1-5 stars)';
COMMENT ON COLUMN ratings.order_id IS 'Required: the completed order that triggered this rating (one rating per order)';

