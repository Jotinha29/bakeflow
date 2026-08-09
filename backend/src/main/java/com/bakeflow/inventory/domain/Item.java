package com.bakeflow.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Item {
    private final UUID id;
    private String name;
    private String sku;
    private String barcode;
    private ItemType type;
    private UnitOfMeasure unit;
    private BigDecimal minimumStock;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public Item(UUID id, String name, String sku, String barcode, ItemType type, UnitOfMeasure unit,
                BigDecimal minimumStock, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        update(name, sku, barcode, type, unit, minimumStock);
        this.active = active;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }
    public static Item create(String name, String sku, String barcode, ItemType type, UnitOfMeasure unit, BigDecimal minimumStock) {
        return new Item(null, name, sku, barcode, type, unit, minimumStock, true, null, null);
    }
    public void update(String name, String sku, String barcode, ItemType type, UnitOfMeasure unit, BigDecimal minimumStock) {
        if (name == null || name.isBlank()) throw new DomainException("Item name is required.");
        if (type == null) throw new DomainException("Item type is required.");
        if (unit == null) throw new DomainException("Item unit is required.");
        if (minimumStock != null && minimumStock.signum() < 0) throw new DomainException("Minimum stock cannot be negative.");
        this.name = name.trim(); this.sku = normalize(sku); this.barcode = normalize(barcode);
        this.type = type; this.unit = unit; this.minimumStock = minimumStock; this.updatedAt = Instant.now();
    }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public void activate() { active = true; updatedAt = Instant.now(); }
    public void deactivate() { active = false; updatedAt = Instant.now(); }
    public UUID id() { return id; } public String name() { return name; } public String sku() { return sku; }
    public String barcode() { return barcode; } public ItemType type() { return type; } public UnitOfMeasure unit() { return unit; }
    public BigDecimal minimumStock() { return minimumStock; } public boolean active() { return active; }
    public Instant createdAt() { return createdAt; } public Instant updatedAt() { return updatedAt; }
}
