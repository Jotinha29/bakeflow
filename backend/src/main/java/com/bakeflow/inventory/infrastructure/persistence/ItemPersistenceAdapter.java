package com.bakeflow.inventory.infrastructure.persistence;

import com.bakeflow.inventory.application.*;
import com.bakeflow.inventory.domain.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public class ItemPersistenceAdapter implements ItemRepository {
    private final ItemJpaRepository repository;
    ItemPersistenceAdapter(ItemJpaRepository repository){this.repository=repository;}
    public Item save(Item item){return repository.save(ItemEntity.from(item)).toDomain();}
    public Optional<Item> findById(UUID id){return repository.findById(id).map(ItemEntity::toDomain);}
    public PageResult<Item> search(String search,String sku,ItemType type,Boolean active,int page,int size){
        Specification<ItemEntity> spec=(root,q,cb)->cb.conjunction();
        if(search!=null&&!search.isBlank())spec=spec.and((r,q,cb)->cb.like(cb.lower(r.get("name")),"%"+search.toLowerCase()+"%"));
        if(sku!=null&&!sku.isBlank())spec=spec.and((r,q,cb)->cb.equal(cb.lower(r.get("sku")),sku.toLowerCase()));
        if(type!=null)spec=spec.and((r,q,cb)->cb.equal(r.get("type"),type));
        if(active!=null)spec=spec.and((r,q,cb)->cb.equal(r.get("active"),active));
        var p=repository.findAll(spec,PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),100)));
        return new PageResult<>(p.getContent().stream().map(ItemEntity::toDomain).toList(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());
    }
    public boolean existsBySku(String sku,UUID id){return id==null?repository.existsBySkuIgnoreCase(sku):repository.existsBySkuIgnoreCaseAndIdNot(sku,id);}
    public boolean existsByBarcode(String barcode,UUID id){return id==null?repository.existsByBarcode(barcode):repository.existsByBarcodeAndIdNot(barcode,id);}
}

interface ItemJpaRepository extends JpaRepository<ItemEntity,UUID>,JpaSpecificationExecutor<ItemEntity>{
    boolean existsBySkuIgnoreCase(String sku); boolean existsBySkuIgnoreCaseAndIdNot(String sku,UUID id);
    boolean existsByBarcode(String barcode); boolean existsByBarcodeAndIdNot(String barcode,UUID id);
}

@Entity @Table(name="items")
class ItemEntity {
    @Id UUID id; @Column(nullable=false,length=160) String name; @Column(length=80) String sku; @Column(length=80) String barcode;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) ItemType type;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) UnitOfMeasure unit;
    @Column(name="minimum_stock",precision=19,scale=3) BigDecimal minimumStock; @Column(nullable=false) boolean active;
    @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="updated_at",nullable=false) Instant updatedAt;
    protected ItemEntity(){}
    static ItemEntity from(Item i){var e=new ItemEntity();e.id=i.id();e.name=i.name();e.sku=i.sku();e.barcode=i.barcode();e.type=i.type();e.unit=i.unit();e.minimumStock=i.minimumStock();e.active=i.active();e.createdAt=i.createdAt();e.updatedAt=i.updatedAt();return e;}
    Item toDomain(){return new Item(id,name,sku,barcode,type,unit,minimumStock,active,createdAt,updatedAt);}
}
