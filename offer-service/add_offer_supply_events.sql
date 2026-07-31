-- Tracks units listed on the marketplace (create + replenishment) for retail sell-through rate.
CREATE TABLE IF NOT EXISTS offer_supply_events (
    id BIGSERIAL PRIMARY KEY,
    offer_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    quantity_units INT NOT NULL CHECK (quantity_units > 0),
    event_type VARCHAR(20) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ose_vendor_recorded ON offer_supply_events (vendor_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_ose_offer_recorded ON offer_supply_events (offer_id, recorded_at);
