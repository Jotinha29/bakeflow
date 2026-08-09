package com.bakeflow.inventory.application;
import com.bakeflow.inventory.domain.Location;
import com.bakeflow.inventory.domain.LocationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface LocationRepository {
    Location save(Location location); Optional<Location> findById(UUID id);
    PageResult<Location> search(String search, LocationType type, Boolean active, int page, int size);
    List<Location> findAll(); boolean existsByCode(String code, UUID excludingId);
}
