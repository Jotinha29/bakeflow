package com.bakeflow.inventory.application;
import com.bakeflow.inventory.domain.Batch;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
public interface BatchRepository {
    Batch save(Batch batch); Optional<Batch> findById(UUID id);
    PageResult<Batch> search(UUID itemId, String code, Boolean active, LocalDate expirationFrom, LocalDate expirationTo, int page, int size);
    boolean existsByItemIdAndCode(UUID itemId, String code, UUID excludingId);
}
