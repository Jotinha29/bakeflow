package com.bakeflow.inventory.domain;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;import org.junit.jupiter.api.Test;

class InventoryDomainTests {
 @Test void rejectsNegativeMinimumStock(){assertThatThrownBy(()->Item.create("Flour",null,null,ItemType.RAW_MATERIAL,UnitOfMeasure.KG,new BigDecimal("-0.01"))).isInstanceOf(DomainException.class).hasMessageContaining("negative");}
 @Test void itemCanBeDeactivatedAndActivated(){var item=Item.create("Flour","FAR-001",null,ItemType.RAW_MATERIAL,UnitOfMeasure.KG,BigDecimal.ZERO);item.deactivate();assertThat(item.active()).isFalse();item.activate();assertThat(item.active()).isTrue();}
 @Test void rejectsExpirationBeforeManufacturing(){var today=LocalDate.now();assertThatThrownBy(()->Batch.create(UUID.randomUUID(),"001",today,today.minusDays(1))).isInstanceOf(DomainException.class).hasMessageContaining("Expiration");}
 @Test void batchCanBeDeactivatedAndActivated(){var batch=Batch.create(UUID.randomUUID(),"001",null,null);batch.deactivate();assertThat(batch.active()).isFalse();batch.activate();assertThat(batch.active()).isTrue();}
 @Test void rejectsSelfParent(){UUID id=UUID.randomUUID();assertThatThrownBy(()->new Location(id,"Shelf","S-1",LocationType.SHELF,id,true,null,null)).isInstanceOf(DomainException.class).hasMessageContaining("own parent");}
 @Test void locationCanBeDeactivatedAndActivated(){var location=Location.create("Shelf","S-1",LocationType.SHELF,null);location.deactivate();assertThat(location.active()).isFalse();location.activate();assertThat(location.active()).isTrue();}
}
