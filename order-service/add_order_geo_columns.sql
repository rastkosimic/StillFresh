-- Snapshot of the vendor/pickup location coordinates on each order, used for the
-- anti-bypass geo-fence check at cancellation time.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS latitude  DOUBLE PRECISION;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
