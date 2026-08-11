package com.bakeflow.inventory.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryStockOperations {
    record Allocation(UUID itemId, String itemName, UUID batchId, String batchCode, UUID locationId,
                      String locationName, LocalDate expirationDate, BigDecimal quantity) {}
    record Availability(UUID itemId, String itemName, BigDecimal required, BigDecimal available,
                        boolean sufficient, List<Allocation> allocations) {}
    record Requirement(UUID itemId, BigDecimal quantity) {}

    List<Availability> preview(List<Requirement> requirements);
    List<Allocation> consumeFefo(List<Requirement> requirements, String reference);
    void receive(UUID itemId, UUID batchId, UUID locationId, BigDecimal quantity, String reference);
}
