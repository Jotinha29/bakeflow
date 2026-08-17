package com.bakeflow.inventory.application;

import com.bakeflow.inventory.domain.UnitOfMeasure;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StockDtos {
    private StockDtos() {}

    public enum EntryReason { RECEIPT, INITIAL_INVENTORY, RETURN, AUTHORIZED_ADJUSTMENT, OTHER }
    public enum ExitReason { INTERNAL_USE, RETURN_TO_SUPPLIER, DONATION, OTHER }
    public enum LossReason { EXPIRED, DAMAGED, CONTAMINATED, PROCESS_LOSS, COUNT_DIFFERENCE, OTHER }
    public enum MovementType {
        ENTRY, EXIT, TRANSFER, LOSS, ADJUSTMENT, PRODUCTION_CONSUMPTION, PRODUCTION_OUTPUT
    }
    public enum ExpirationStatus { VALID, EXPIRING_SOON, EXPIRED, WITHOUT_EXPIRATION }

    public record EntryRequest(
            @NotNull UUID itemId,
            @NotNull UUID batchId,
            @NotNull UUID locationId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotNull EntryReason reason,
            @Size(max = 1000) String notes) {}

    public record ExitRequest(
            @NotNull UUID itemId,
            @NotNull UUID batchId,
            @NotNull UUID locationId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotNull ExitReason reason,
            @Size(max = 1000) String notes) {}

    public record TransferRequest(
            @NotNull UUID itemId,
            @NotNull UUID batchId,
            @NotNull UUID sourceLocationId,
            @NotNull UUID destinationLocationId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @Size(max = 1000) String notes) {}

    public record LossRequest(
            @NotNull UUID itemId,
            @NotNull UUID batchId,
            @NotNull UUID locationId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotNull LossReason reason,
            @Size(max = 1000) String notes) {}

    public record AdjustmentRequest(
            @NotNull UUID itemId,
            @NotNull UUID batchId,
            @NotNull UUID locationId,
            @NotNull @DecimalMin(value = "0.000") BigDecimal physicalQuantity,
            @NotBlank @Size(max = 1000) String justification) {}

    public record BalanceView(
            UUID id, UUID itemId, String itemName, String sku, UUID batchId, String batchCode,
            LocalDate expirationDate, ExpirationStatus expirationStatus, UUID locationId,
            String locationName, BigDecimal quantity, UnitOfMeasure unit, Instant updatedAt) {}

    public record BalancePage(List<BalanceView> content, int page, int size, long totalElements,
            int totalPages) {}

    public record MovementView(
            UUID id, MovementType type, UUID itemId, String itemName, String sku, UUID batchId,
            String batchCode, UUID sourceLocationId, String sourceLocationName,
            UUID destinationLocationId, String destinationLocationName, BigDecimal quantity,
            UnitOfMeasure unit, UUID actorUserId, String actorName, String reason, String notes,
            String reference, BigDecimal previousQuantity, BigDecimal resultingQuantity,
            Instant createdAt) {}

    public record MovementPage(List<MovementView> content, int page, int size, long totalElements,
            int totalPages) {}
}
