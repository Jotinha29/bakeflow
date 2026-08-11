package com.bakeflow.production.application;

import com.bakeflow.inventory.application.InventoryStockOperations.Allocation;
import com.bakeflow.inventory.application.InventoryStockOperations.Availability;
import com.bakeflow.inventory.domain.UnitOfMeasure;
import com.bakeflow.production.domain.ProductionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProductionDtos {
    private ProductionDtos() {}
    public record IngredientInput(UUID itemId, BigDecimal quantity, UnitOfMeasure unit) {}
    public record RecipeInput(String name, UUID outputItemId, BigDecimal yieldQuantity, UnitOfMeasure yieldUnit,
                              Integer shelfLifeDays, boolean active, String notes, List<IngredientInput> ingredients) {}
    public record IngredientView(UUID id, UUID itemId, String itemName, BigDecimal quantity, UnitOfMeasure unit) {}
    public record RecipeView(UUID id, String name, UUID outputItemId, String outputItemName, BigDecimal yieldQuantity,
                             UnitOfMeasure yieldUnit, Integer shelfLifeDays, boolean active, String notes,
                             List<IngredientView> ingredients, Instant createdAt, Instant updatedAt) {}
    public record OrderInput(UUID recipeId, BigDecimal plannedQuantity, LocalDate plannedDate, String notes) {}
    public record PreviewInput(UUID recipeId, BigDecimal plannedQuantity) {}
    public record ProductionPreview(RecipeView recipe, BigDecimal plannedQuantity, List<RequirementView> requirements) {}
    public record CompleteInput(BigDecimal actualQuantity, UUID destinationLocationId, String differenceReason, String notes) {}
    public record RequirementView(UUID itemId, String itemName, BigDecimal required, BigDecimal available,
                                  UnitOfMeasure unit, boolean sufficient, List<Allocation> allocations) {
        static RequirementView from(Availability value, UnitOfMeasure unit) {
            return new RequirementView(value.itemId(), value.itemName(), value.required(), value.available(), unit,
                value.sufficient(), value.allocations());
        }
    }
    public record OutputView(UUID itemId, String itemName, UUID batchId, String batchCode, UUID locationId,
                             String locationName, BigDecimal quantity) {}
    public record ProductionOrderView(UUID id, String code, UUID recipeId, String recipeName, UUID outputItemId,
                                      String outputItemName, BigDecimal plannedQuantity, BigDecimal actualQuantity,
                                      UnitOfMeasure unit, ProductionStatus status, LocalDate plannedDate, Instant startedAt,
                                      Instant completedAt, String differenceReason, String notes,
                                      List<RequirementView> requirements, List<Allocation> consumptions, OutputView output,
                                      Instant createdAt, Instant updatedAt) {}
    public record ProductionSummary(long planned, long inProgress, long completedToday, List<ProductionOrderView> recent) {}
}
