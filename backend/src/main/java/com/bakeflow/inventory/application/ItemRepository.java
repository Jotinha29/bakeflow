package com.bakeflow.inventory.application;
import com.bakeflow.inventory.domain.Item;
import com.bakeflow.inventory.domain.ItemType;
import java.util.Optional;
import java.util.UUID;
public interface ItemRepository {
    Item save(Item item); Optional<Item> findById(UUID id);
    PageResult<Item> search(String search, String sku, ItemType type, Boolean active, int page, int size);
    boolean existsBySku(String sku, UUID excludingId); boolean existsByBarcode(String barcode, UUID excludingId);
}
