ALTER TABLE items ADD COLUMN shelf_life_days INTEGER;
ALTER TABLE items ADD CONSTRAINT ck_items_shelf_life CHECK (shelf_life_days IS NULL OR shelf_life_days >= 0);

CREATE TABLE stock_balances (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES items(id),
    batch_id UUID NOT NULL REFERENCES batches(id),
    location_id UUID NOT NULL REFERENCES locations(id),
    quantity NUMERIC(19, 3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_stock_balance UNIQUE (batch_id, location_id),
    CONSTRAINT ck_stock_quantity CHECK (quantity >= 0)
);
CREATE INDEX idx_stock_item_quantity ON stock_balances (item_id, quantity);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES items(id),
    batch_id UUID NOT NULL REFERENCES batches(id),
    location_id UUID NOT NULL REFERENCES locations(id),
    type VARCHAR(40) NOT NULL,
    quantity NUMERIC(19, 3) NOT NULL,
    reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_stock_movement_quantity CHECK (quantity > 0)
);
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference);
CREATE INDEX idx_stock_movements_item_created ON stock_movements (item_id, created_at);

CREATE TABLE recipes (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    output_item_id UUID NOT NULL REFERENCES items(id),
    yield_quantity NUMERIC(19, 3) NOT NULL,
    yield_unit VARCHAR(16) NOT NULL,
    shelf_life_days INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_recipe_yield CHECK (yield_quantity > 0),
    CONSTRAINT ck_recipe_shelf_life CHECK (shelf_life_days IS NULL OR shelf_life_days >= 0)
);
CREATE INDEX idx_recipes_name ON recipes (name);
CREATE INDEX idx_recipes_output_active ON recipes (output_item_id, active);

CREATE TABLE recipe_ingredients (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES items(id),
    quantity NUMERIC(19, 3) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    CONSTRAINT uk_recipe_ingredient UNIQUE (recipe_id, item_id),
    CONSTRAINT ck_recipe_ingredient_quantity CHECK (quantity > 0)
);

CREATE SEQUENCE production_order_code_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE production_orders (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    recipe_id UUID NOT NULL REFERENCES recipes(id),
    planned_quantity NUMERIC(19, 3) NOT NULL,
    actual_quantity NUMERIC(19, 3),
    status VARCHAR(24) NOT NULL,
    planned_date DATE NOT NULL,
    destination_location_id UUID REFERENCES locations(id),
    difference_reason VARCHAR(500),
    notes VARCHAR(1000),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_production_planned CHECK (planned_quantity > 0),
    CONSTRAINT ck_production_actual CHECK (actual_quantity IS NULL OR actual_quantity >= 0)
);
CREATE INDEX idx_production_orders_status_date ON production_orders (status, planned_date);
CREATE INDEX idx_production_orders_recipe ON production_orders (recipe_id);

CREATE TABLE production_consumptions (
    id UUID PRIMARY KEY,
    production_order_id UUID NOT NULL REFERENCES production_orders(id),
    item_id UUID NOT NULL REFERENCES items(id),
    batch_id UUID NOT NULL REFERENCES batches(id),
    location_id UUID NOT NULL REFERENCES locations(id),
    quantity NUMERIC(19, 3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_production_consumption_quantity CHECK (quantity > 0)
);
CREATE INDEX idx_production_consumptions_order ON production_consumptions (production_order_id);

CREATE TABLE production_outputs (
    id UUID PRIMARY KEY,
    production_order_id UUID NOT NULL UNIQUE REFERENCES production_orders(id),
    item_id UUID NOT NULL REFERENCES items(id),
    batch_id UUID NOT NULL REFERENCES batches(id),
    location_id UUID NOT NULL REFERENCES locations(id),
    quantity NUMERIC(19, 3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_production_output_quantity CHECK (quantity >= 0)
);
