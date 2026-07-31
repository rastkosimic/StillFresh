-- Order-service migration: add persisted snapshot columns to the `orders` table.
--
-- These columns capture vendor/offer display info at order-creation time so
-- order history/detail pages remain self-contained even if the underlying
-- vendor or offer is later modified, deleted, or expires.
--
-- Hibernate `ddl-auto: update` will add these columns automatically on startup.
-- This file is provided for environments where Hibernate DDL is disabled or
-- for manual, auditable migration.
--
-- Run against: order_db (PostgreSQL)

ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS location_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS chain_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS website VARCHAR(500),
  ADD COLUMN IF NOT EXISTS vendor_image_url VARCHAR(500),
  ADD COLUMN IF NOT EXISTS offer_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS offer_image_url VARCHAR(500),
  ADD COLUMN IF NOT EXISTS address VARCHAR(255),
  ADD COLUMN IF NOT EXISTS zip_code VARCHAR(32);
