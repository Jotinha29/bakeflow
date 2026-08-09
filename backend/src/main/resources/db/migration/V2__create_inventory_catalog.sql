CREATE TABLE items (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    sku VARCHAR(80),
    barcode VARCHAR(80),
    type VARCHAR(32) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    minimum_stock NUMERIC(19, 3),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_items_sku UNIQUE (sku),
    CONSTRAINT uk_items_barcode UNIQUE (barcode),
    CONSTRAINT ck_items_minimum_stock CHECK (minimum_stock IS NULL OR minimum_stock >= 0)
);

CREATE INDEX idx_items_name ON items (name);
CREATE INDEX idx_items_type_active ON items (type, active);

CREATE TABLE batches (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES items(id),
    code VARCHAR(80) NOT NULL,
    manufacturing_date DATE,
    expiration_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_batches_item_code UNIQUE (item_id, code),
    CONSTRAINT ck_batches_dates CHECK (manufacturing_date IS NULL OR expiration_date IS NULL OR expiration_date >= manufacturing_date)
);

CREATE INDEX idx_batches_item_active ON batches (item_id, active);
CREATE INDEX idx_batches_expiration_date ON batches (expiration_date);

CREATE TABLE locations (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(80) NOT NULL,
    type VARCHAR(32) NOT NULL,
    parent_id UUID REFERENCES locations(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_locations_code UNIQUE (code),
    CONSTRAINT ck_locations_not_self_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE INDEX idx_locations_parent ON locations (parent_id);
CREATE INDEX idx_locations_type_active ON locations (type, active);
