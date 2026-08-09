package com.bakeflow.inventory.infrastructure.persistence;

import com.bakeflow.inventory.application.*;
import com.bakeflow.inventory.domain.Batch;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public class BatchPersistenceAdapter implements BatchRepository {
    private final BatchJpaRepository repository; BatchPersistenceAdapter(BatchJpaRepository repository){this.repository=repository;}
    public Batch save(Batch b){return repository.save(BatchEntity.from(b)).toDomain();}
    public Optional<Batch> findById(UUID id){return repository.findById(id).map(BatchEntity::toDomain);}
    public PageResult<Batch> search(UUID itemId,String code,Boolean active,LocalDate from,LocalDate to,int page,int size){
        Specification<BatchEntity>s=(r,q,cb)->cb.conjunction();
        if(itemId!=null)s=s.and((r,q,cb)->cb.equal(r.get("itemId"),itemId));
        if(code!=null&&!code.isBlank())s=s.and((r,q,cb)->cb.like(cb.lower(r.get("code")),"%"+code.toLowerCase()+"%"));
        if(active!=null)s=s.and((r,q,cb)->cb.equal(r.get("active"),active));
        if(from!=null)s=s.and((r,q,cb)->cb.greaterThanOrEqualTo(r.get("expirationDate"),from));
        if(to!=null)s=s.and((r,q,cb)->cb.lessThanOrEqualTo(r.get("expirationDate"),to));
        var p=repository.findAll(s,PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),100)));
        return new PageResult<>(p.getContent().stream().map(BatchEntity::toDomain).toList(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());
    }
    public boolean existsByItemIdAndCode(UUID itemId,String code,UUID id){return id==null?repository.existsByItemIdAndCodeIgnoreCase(itemId,code):repository.existsByItemIdAndCodeIgnoreCaseAndIdNot(itemId,code,id);}
}
interface BatchJpaRepository extends JpaRepository<BatchEntity,UUID>,JpaSpecificationExecutor<BatchEntity>{boolean existsByItemIdAndCodeIgnoreCase(UUID itemId,String code);boolean existsByItemIdAndCodeIgnoreCaseAndIdNot(UUID itemId,String code,UUID id);}
@Entity @Table(name="batches") class BatchEntity{
    @Id UUID id;@Column(name="item_id",nullable=false)UUID itemId;@Column(nullable=false,length=80)String code;
    @Column(name="manufacturing_date")LocalDate manufacturingDate;@Column(name="expiration_date")LocalDate expirationDate;
    @Column(nullable=false)boolean active;@Column(name="created_at",nullable=false)Instant createdAt;@Column(name="updated_at",nullable=false)Instant updatedAt;
    protected BatchEntity(){}static BatchEntity from(Batch b){var e=new BatchEntity();e.id=b.id();e.itemId=b.itemId();e.code=b.code();e.manufacturingDate=b.manufacturingDate();e.expirationDate=b.expirationDate();e.active=b.active();e.createdAt=b.createdAt();e.updatedAt=b.updatedAt();return e;}
    Batch toDomain(){return new Batch(id,itemId,code,manufacturingDate,expirationDate,active,createdAt,updatedAt);}
}
