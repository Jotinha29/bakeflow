package com.bakeflow.inventory.domain;

import java.time.Instant;
import java.util.UUID;

public class Location {
    private final UUID id; private String name; private String code; private LocationType type; private UUID parentId;
    private boolean active; private final Instant createdAt; private Instant updatedAt;
    public Location(UUID id, String name, String code, LocationType type, UUID parentId, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id == null ? UUID.randomUUID() : id; this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.active = active; update(name, code, type, parentId); this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }
    public static Location create(String name, String code, LocationType type, UUID parentId) { return new Location(null, name, code, type, parentId, true, null, null); }
    public void update(String name, String code, LocationType type, UUID parentId) {
        if (name == null || name.isBlank()) throw new DomainException("Location name is required.");
        if (code == null || code.isBlank()) throw new DomainException("Location code is required.");
        if (type == null) throw new DomainException("Location type is required.");
        if (id.equals(parentId)) throw new DomainException("A location cannot be its own parent.");
        this.name = name.trim(); this.code = code.trim(); this.type = type; this.parentId = parentId; this.updatedAt = Instant.now();
    }
    public void activate() { active = true; updatedAt = Instant.now(); } public void deactivate() { active = false; updatedAt = Instant.now(); }
    public UUID id() { return id; } public String name() { return name; } public String code() { return code; }
    public LocationType type() { return type; } public UUID parentId() { return parentId; } public boolean active() { return active; }
    public Instant createdAt() { return createdAt; } public Instant updatedAt() { return updatedAt; }
}
