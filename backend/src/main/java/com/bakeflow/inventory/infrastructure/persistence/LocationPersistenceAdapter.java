package com.bakeflow.inventory.infrastructure.persistence;

import com.bakeflow.inventory.application.*;import com.bakeflow.inventory.domain.*;import jakarta.persistence.*;
import java.time.Instant;import java.util.*;import org.springframework.data.domain.PageRequest;import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;import org.springframework.stereotype.Repository;

@Repository public class LocationPersistenceAdapter implements LocationRepository{
    private final LocationJpaRepository repository;LocationPersistenceAdapter(LocationJpaRepository repository){this.repository=repository;}
    public Location save(Location l){return repository.save(LocationEntity.from(l)).toDomain();}public Optional<Location>findById(UUID id){return repository.findById(id).map(LocationEntity::toDomain);}
    public PageResult<Location>search(String search,LocationType type,Boolean active,int page,int size){Specification<LocationEntity>s=(r,q,cb)->cb.conjunction();if(search!=null&&!search.isBlank())s=s.and((r,q,cb)->cb.like(cb.lower(r.get("name")),"%"+search.toLowerCase()+"%"));if(type!=null)s=s.and((r,q,cb)->cb.equal(r.get("type"),type));if(active!=null)s=s.and((r,q,cb)->cb.equal(r.get("active"),active));var p=repository.findAll(s,PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),100)));return new PageResult<>(p.getContent().stream().map(LocationEntity::toDomain).toList(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
    public List<Location>findAll(){return repository.findAll().stream().map(LocationEntity::toDomain).toList();}public boolean existsByCode(String code,UUID id){return id==null?repository.existsByCodeIgnoreCase(code):repository.existsByCodeIgnoreCaseAndIdNot(code,id);}
}
interface LocationJpaRepository extends JpaRepository<LocationEntity,UUID>,JpaSpecificationExecutor<LocationEntity>{boolean existsByCodeIgnoreCase(String code);boolean existsByCodeIgnoreCaseAndIdNot(String code,UUID id);}
@Entity @Table(name="locations")class LocationEntity{@Id UUID id;@Column(nullable=false,length=160)String name;@Column(nullable=false,length=80)String code;@Enumerated(EnumType.STRING)@Column(nullable=false,length=32)LocationType type;@Column(name="parent_id")UUID parentId;@Column(nullable=false)boolean active;@Column(name="created_at",nullable=false)Instant createdAt;@Column(name="updated_at",nullable=false)Instant updatedAt;protected LocationEntity(){}static LocationEntity from(Location l){var e=new LocationEntity();e.id=l.id();e.name=l.name();e.code=l.code();e.type=l.type();e.parentId=l.parentId();e.active=l.active();e.createdAt=l.createdAt();e.updatedAt=l.updatedAt();return e;}Location toDomain(){return new Location(id,name,code,type,parentId,active,createdAt,updatedAt);}}
