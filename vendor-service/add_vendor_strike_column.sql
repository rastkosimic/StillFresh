-- Anti-abuse strike counter for vendors flagged in potential user-vendor bypass scams.
ALTER TABLE vendor ADD COLUMN IF NOT EXISTS bypass_strike_count INTEGER NOT NULL DEFAULT 0;
