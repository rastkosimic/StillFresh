-- Anti-abuse strike counters used by the fraud/no-show engine.
ALTER TABLE users ADD COLUMN IF NOT EXISTS bypass_strike_count  INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS no_show_strike_count INTEGER NOT NULL DEFAULT 0;
