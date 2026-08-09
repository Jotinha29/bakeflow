package com.bakeflow.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Batch {
    private final UUID id; private final UUID itemId; private String code; private LocalDate manufacturingDate;
    private LocalDate expirationDate; private boolean active; private final Instant createdAt; private Instant updatedAt;
    public Batch(UUID id, UUID itemId, String code, LocalDate manufacturingDate, LocalDate expirationDate,
                 boolean active, Instant createdAt, Instant updatedAt) {
        if (itemId == null) throw new DomainException("Batch item is required.");
        this.id = id == null ? UUID.randomUUID() : id; this.itemId = itemId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt; this.active = active;
        update(code, manufacturingDate, expirationDate); this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }
    public static Batch create(UUID itemId, String code, LocalDate manufacturingDate, LocalDate expirationDate) {
        return new Batch(null, itemId, code, manufacturingDate, expirationDate, true, null, null);
    }
    public void update(String code, LocalDate manufacturingDate, LocalDate expirationDate) {
        if (code == null || code.isBlank()) throw new DomainException("Batch code is required.");
        if (manufacturingDate != null && expirationDate != null && expirationDate.isBefore(manufacturingDate))
            throw new DomainException("Expiration date cannot be before manufacturing date.");
        this.code = code.trim(); this.manufacturingDate = manufacturingDate; this.expirationDate = expirationDate; this.updatedAt = Instant.now();
    }
    public void activate() { active = true; updatedAt = Instant.now(); } public void deactivate() { active = false; updatedAt = Instant.now(); }
    public UUID id() { return id; } public UUID itemId() { return itemId; } public String code() { return code; }
    public LocalDate manufacturingDate() { return manufacturingDate; } public LocalDate expirationDate() { return expirationDate; }
    public boolean active() { return active; } public Instant createdAt() { return createdAt; } public Instant updatedAt() { return updatedAt; }
}
