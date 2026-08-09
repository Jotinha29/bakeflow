package com.bakeflow.inventory.application;

import com.bakeflow.inventory.application.InventoryDtos.*;
import com.bakeflow.inventory.domain.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryCatalogService {
    private final ItemRepository items; private final BatchRepository batches; private final LocationRepository locations;
    public InventoryCatalogService(ItemRepository items, BatchRepository batches, LocationRepository locations) {
        this.items = items; this.batches = batches; this.locations = locations;
    }
    public ItemView createItem(ItemInput input) { validateItemUnique(input.sku(), input.barcode(), null); return ItemView.from(items.save(Item.create(input.name(), input.sku(), input.barcode(), input.type(), input.unit(), input.minimumStock()))); }
    public ItemView updateItem(UUID id, ItemInput input) { Item item = item(id); validateItemUnique(input.sku(), input.barcode(), id); item.update(input.name(), input.sku(), input.barcode(), input.type(), input.unit(), input.minimumStock()); return ItemView.from(items.save(item)); }
    @Transactional(readOnly = true) public ItemView getItem(UUID id) { return ItemView.from(item(id)); }
    @Transactional(readOnly = true) public PageResult<ItemView> searchItems(String q, String sku, ItemType type, Boolean active, int page, int size) { var p=items.search(q,sku,type,active,page,size); return mapPage(p, p.content().stream().map(ItemView::from).toList()); }
    public ItemView setItemActive(UUID id, boolean active) { Item item=item(id); if(active)item.activate();else item.deactivate(); return ItemView.from(items.save(item)); }
    private void validateItemUnique(String sku, String barcode, UUID id) { if(sku!=null&&!sku.isBlank()&&items.existsBySku(sku.trim(),id)) throw new DomainException("SKU already exists."); if(barcode!=null&&!barcode.isBlank()&&items.existsByBarcode(barcode.trim(),id)) throw new DomainException("Barcode already exists."); }
    private Item item(UUID id) { return items.findById(id).orElseThrow(() -> new DomainException("Item not found.")); }

    public BatchView createBatch(BatchInput input) { item(input.itemId()); validateBatchUnique(input.itemId(),input.code(),null); return batchView(batches.save(Batch.create(input.itemId(),input.code(),input.manufacturingDate(),input.expirationDate()))); }
    public BatchView updateBatch(UUID id, BatchInput input) { Batch batch=batch(id); if(!batch.itemId().equals(input.itemId())) throw new DomainException("Batch item cannot be changed."); validateBatchUnique(input.itemId(),input.code(),id); batch.update(input.code(),input.manufacturingDate(),input.expirationDate()); return batchView(batches.save(batch)); }
    @Transactional(readOnly=true) public BatchView getBatch(UUID id){return batchView(batch(id));}
    @Transactional(readOnly=true) public PageResult<BatchView> searchBatches(UUID itemId,String code,Boolean active,LocalDate from,LocalDate to,int page,int size){var p=batches.search(itemId,code,active,from,to,page,size);return mapPage(p,p.content().stream().map(this::batchView).toList());}
    public BatchView setBatchActive(UUID id,boolean active){Batch b=batch(id);if(active)b.activate();else b.deactivate();return batchView(batches.save(b));}
    private Batch batch(UUID id){return batches.findById(id).orElseThrow(()->new DomainException("Batch not found."));}
    private void validateBatchUnique(UUID itemId,String code,UUID id){if(code!=null&&batches.existsByItemIdAndCode(itemId,code.trim(),id))throw new DomainException("Batch code already exists for this item.");}
    private BatchView batchView(Batch b){return new BatchView(b.id(),b.itemId(),item(b.itemId()).name(),b.code(),b.manufacturingDate(),b.expirationDate(),b.active(),b.createdAt(),b.updatedAt());}

    public LocationView createLocation(LocationInput input){validateLocation(input.parentId(),null);if(locations.existsByCode(input.code(),null))throw new DomainException("Location code already exists.");return locationView(locations.save(Location.create(input.name(),input.code(),input.type(),input.parentId())),List.of());}
    public LocationView updateLocation(UUID id,LocationInput input){Location location=location(id);validateLocation(input.parentId(),id);if(locations.existsByCode(input.code(),id))throw new DomainException("Location code already exists.");location.update(input.name(),input.code(),input.type(),input.parentId());return locationView(locations.save(location),List.of());}
    @Transactional(readOnly=true) public LocationView getLocation(UUID id){return locationView(location(id),List.of());}
    @Transactional(readOnly=true) public PageResult<LocationView> searchLocations(String q,LocationType type,Boolean active,int page,int size){var p=locations.search(q,type,active,page,size);return mapPage(p,p.content().stream().map(l->locationView(l,List.of())).toList());}
    @Transactional(readOnly=true) public List<LocationView> locationTree(){List<Location> all=locations.findAll();Map<UUID,List<Location>> children=new HashMap<>();for(Location l:all)children.computeIfAbsent(l.parentId(),k->new ArrayList<>()).add(l);return buildTree(children,null,new HashSet<>());}
    public LocationView setLocationActive(UUID id,boolean active){Location l=location(id);if(active)l.activate();else l.deactivate();return locationView(locations.save(l),List.of());}
    private void validateLocation(UUID parentId,UUID id){if(parentId==null)return;if(parentId.equals(id))throw new DomainException("A location cannot be its own parent.");Location cursor=location(parentId);Set<UUID>seen=new HashSet<>();while(cursor!=null){if(cursor.id().equals(id))throw new DomainException("Location hierarchy cannot contain cycles.");if(!seen.add(cursor.id()))throw new DomainException("Location hierarchy contains a cycle.");cursor=cursor.parentId()==null?null:location(cursor.parentId());}}
    private Location location(UUID id){return locations.findById(id).orElseThrow(()->new DomainException("Location not found."));}
    private List<LocationView> buildTree(Map<UUID,List<Location>> grouped,UUID parent,Set<UUID>path){return grouped.getOrDefault(parent,List.of()).stream().sorted(Comparator.comparing(Location::name)).map(l->{if(!path.add(l.id()))throw new DomainException("Location hierarchy contains a cycle.");var c=buildTree(grouped,l.id(),new HashSet<>(path));return locationView(l,c);}).toList();}
    private LocationView locationView(Location l,List<LocationView> children){return new LocationView(l.id(),l.name(),l.code(),l.type(),l.parentId(),l.active(),l.createdAt(),l.updatedAt(),children);}
    private <A,B> PageResult<B> mapPage(PageResult<A> page,List<B> content){return new PageResult<>(content,page.page(),page.size(),page.totalElements(),page.totalPages());}
}
