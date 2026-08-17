ALTER TABLE stock_movements ADD COLUMN source_location_id UUID REFERENCES locations(id);
ALTER TABLE stock_movements ADD COLUMN destination_location_id UUID REFERENCES locations(id);
ALTER TABLE stock_movements ADD COLUMN actor_user_id UUID REFERENCES users(id);
ALTER TABLE stock_movements ADD COLUMN reason VARCHAR(50);
ALTER TABLE stock_movements ADD COLUMN notes VARCHAR(1000);
ALTER TABLE stock_movements ADD COLUMN previous_quantity NUMERIC(19, 3);
ALTER TABLE stock_movements ADD COLUMN resulting_quantity NUMERIC(19, 3);

UPDATE stock_movements
SET source_location_id = CASE WHEN type = 'PRODUCTION_CONSUMPTION' THEN location_id END,
    destination_location_id = CASE WHEN type = 'PRODUCTION_OUTPUT' THEN location_id END;

CREATE INDEX idx_stock_balances_updated ON stock_balances(updated_at DESC);
CREATE INDEX idx_stock_movements_created ON stock_movements(created_at DESC);
CREATE INDEX idx_stock_movements_batch_created ON stock_movements(batch_id, created_at DESC);
CREATE INDEX idx_stock_movements_locations ON stock_movements(source_location_id, destination_location_id);
CREATE INDEX idx_stock_movements_actor ON stock_movements(actor_user_id);

INSERT INTO permissions(id, code) VALUES
('72000000-0000-0000-0000-000000000018', 'STOCK_ADJUSTMENT'),
('72000000-0000-0000-0000-000000000019', 'MOVEMENT_READ');

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('STOCK_ADJUSTMENT', 'MOVEMENT_READ');

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'MANAGER' AND p.code IN ('STOCK_ADJUSTMENT', 'MOVEMENT_READ');

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('OPERATOR', 'VIEWER') AND p.code = 'MOVEMENT_READ';
