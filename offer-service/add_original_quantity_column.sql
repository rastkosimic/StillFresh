-- Migration: add original_quantity to offers table
-- This column captures the quantity when an offer was first published.
-- It is never updated, enabling sell-through rate calculation.
-- Backfill existing rows with their current quantityAvailable as best approximation.

ALTER TABLE offers ADD COLUMN IF NOT EXISTS original_quantity INT NOT NULL DEFAULT 0;

UPDATE offers SET original_quantity = quantity_available WHERE original_quantity = 0;
