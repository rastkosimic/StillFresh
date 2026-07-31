-- Backfill one CREATE supply event per existing offer for sell-through analytics.
INSERT INTO offer_supply_events (offer_id, vendor_id, quantity_units, event_type, recorded_at)
SELECT o.id,
       o.vendor_id,
       CASE WHEN o.original_quantity > 0 THEN o.original_quantity ELSE o.quantity_available END,
       'CREATE',
       COALESCE(o.created_at, NOW())
FROM offers o
WHERE CASE WHEN o.original_quantity > 0 THEN o.original_quantity ELSE o.quantity_available END > 0
  AND NOT EXISTS (
      SELECT 1 FROM offer_supply_events e WHERE e.offer_id = o.id AND e.event_type = 'CREATE'
  );
