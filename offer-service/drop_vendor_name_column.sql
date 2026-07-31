-- Offer-service migration: replace legacy `vendor_name` snapshot column with
-- the richer vendor display snapshot (location_name, chain_name, website, vendor_image_url).
--
-- Hibernate `ddl-auto: update` will add new columns automatically on startup.
-- However, `update` never drops columns, so the legacy `vendor_name` column
-- must be removed manually via this script.
--
-- Run against: offer_db (PostgreSQL)

ALTER TABLE offers
  ADD COLUMN IF NOT EXISTS location_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS chain_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS website VARCHAR(500),
  ADD COLUMN IF NOT EXISTS vendor_image_url VARCHAR(500);

ALTER TABLE offers DROP COLUMN IF EXISTS vendor_name;
