-- Migration script to add expiredAt and soldOutAt date tracking fields
-- These fields track when offers expired or were sold out (in vendor's timezone)
-- Only offers that expired or sold out TODAY will be shown to users (greyed out)

-- Add expiredAt column (nullable, will be set when offer expires)
ALTER TABLE offers ADD COLUMN expired_at DATE NULL;

-- Add soldOutAt column (nullable, will be set when offer sells out)
ALTER TABLE offers ADD COLUMN sold_out_at DATE NULL;

-- Add index for efficient querying of today's expired/sold-out offers
CREATE INDEX idx_offers_expired_at ON offers(expired_at) WHERE expired_at IS NOT NULL;
CREATE INDEX idx_offers_sold_out_at ON offers(sold_out_at) WHERE sold_out_at IS NOT NULL;

-- Note: No backfill needed - these fields will be populated as offers expire/sell out going forward

