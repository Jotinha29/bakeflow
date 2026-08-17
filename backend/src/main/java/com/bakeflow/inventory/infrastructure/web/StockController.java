package com.bakeflow.inventory.infrastructure.web;

import static com.bakeflow.inventory.application.StockDtos.*;

import com.bakeflow.inventory.application.StockService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {
    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAuthority('SCOPE_STOCK_READ')")
    BalancePage balances(@RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) ExpirationStatus expiration,
            @RequestParam(defaultValue = "item") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.balances(itemId, sku, batch, locationId, expiration, sort, direction, page, size);
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('SCOPE_STOCK_ENTRY')")
    MovementView entry(@Valid @RequestBody EntryRequest request) {
        return service.entry(request);
    }

    @PostMapping("/exits")
    @PreAuthorize("hasAuthority('SCOPE_STOCK_EXIT')")
    MovementView exit(@Valid @RequestBody ExitRequest request) {
        return service.exit(request);
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAuthority('SCOPE_STOCK_TRANSFER')")
    MovementView transfer(@Valid @RequestBody TransferRequest request) {
        return service.transfer(request);
    }

    @PostMapping("/losses")
    @PreAuthorize("hasAuthority('SCOPE_STOCK_LOSS')")
    MovementView loss(@Valid @RequestBody LossRequest request) {
        return service.loss(request);
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('SCOPE_STOCK_ADJUSTMENT')")
    MovementView adjustment(@Valid @RequestBody AdjustmentRequest request) {
        return service.adjustment(request);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('SCOPE_MOVEMENT_READ')")
    MovementPage movements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.movements(startDate, endDate, type, itemId, batchId, locationId, actorUserId,
                page, size);
    }

    @GetMapping("/movements/{id}")
    @PreAuthorize("hasAuthority('SCOPE_MOVEMENT_READ')")
    MovementView movement(@PathVariable UUID id) {
        return service.movement(id);
    }
}
