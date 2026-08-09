package com.bakeflow.inventory.application;

import com.bakeflow.inventory.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InventoryDtos {
    private InventoryDtos() {}
    public record ItemInput(String name, String sku, String barcode, ItemType type, UnitOfMeasure unit, BigDecimal minimumStock) {}
    public record ItemView(UUID id, String name, String sku, String barcode, ItemType type, UnitOfMeasure unit,
                           BigDecimal minimumStock, boolean active, Instant createdAt, Instant updatedAt) {
        public static ItemView from(Item i) { return new ItemView(i.id(), i.name(), i.sku(), i.barcode(), i.type(), i.unit(), i.minimumStock(), i.active(), i.createdAt(), i.updatedAt()); }
    }
    public record BatchInput(UUID itemId, String code, LocalDate manufacturingDate, LocalDate expirationDate) {}
    public record BatchView(UUID id, UUID itemId, String itemName, String code, LocalDate manufacturingDate,
                            LocalDate expirationDate, boolean active, Instant createdAt, Instant updatedAt) {}
    public record LocationInput(String name, String code, LocationType type, UUID parentId) {}
    public record LocationView(UUID id, String name, String code, LocationType type, UUID parentId, boolean active,
                               Instant createdAt, Instant updatedAt, List<LocationView> children) {}
    public enum LookupStatus { FOUND, NOT_FOUND, UNAVAILABLE }
    public record ProductInformation(LookupStatus status, String barcode, String name, String brand, String imageUrl,
                                     String quantity, List<String> categories, String message) {}
}
