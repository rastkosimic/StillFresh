-- Runtime-customizable platform settings (key/value, current value only).
-- Seeds the global platform fee percentage to 10%.
CREATE TABLE IF NOT EXISTS platform_settings (
    setting_key   VARCHAR(64)  PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
);

INSERT INTO platform_settings (setting_key, setting_value)
VALUES ('fee_percent', '10.0')
ON CONFLICT (setting_key) DO NOTHING;
