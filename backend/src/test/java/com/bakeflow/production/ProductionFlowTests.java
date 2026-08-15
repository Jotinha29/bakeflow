package com.bakeflow.production;

import static com.bakeflow.production.application.ProductionDtos.*;
import static org.assertj.core.api.Assertions.*;

import com.bakeflow.inventory.domain.DomainException;
import com.bakeflow.inventory.domain.UnitOfMeasure;
import com.bakeflow.production.application.ProductionService;
import com.bakeflow.production.domain.ProductionStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest @ActiveProfiles("test")
class ProductionFlowTests {
    @Autowired ProductionService service; @Autowired JdbcTemplate jdbc;
    UUID flour=UUID.randomUUID(),bread=UUID.randomUUID(),location=UUID.randomUUID(),early=UUID.randomUUID(),late=UUID.randomUUID();
    @BeforeEach void setup(){Instant now=Instant.now();
        jdbc.update("INSERT INTO items(id,name,type,unit,active,created_at,updated_at) VALUES(?,?,'RAW_MATERIAL','KG',true,?,?)",flour,"Farinha",now,now);
        jdbc.update("INSERT INTO items(id,name,type,unit,active,created_at,updated_at) VALUES(?,?,'FINISHED_PRODUCT','UNIT',true,?,?)",bread,"Pão",now,now);
        jdbc.update("INSERT INTO locations(id,name,code,type,active,created_at,updated_at) VALUES(?,?,?,'WAREHOUSE',true,?,?)",location,"Estoque","LOC-"+location,now,now);
        batch(early,"LOTE-CEDO",LocalDate.now().plusDays(2),now.minusSeconds(10));batch(late,"LOTE-TARDE",LocalDate.now().plusDays(10),now);
        balance(early,new BigDecimal("10"));balance(late,new BigDecimal("30"));
    }
    @Test void completesFlowUsingFefoAndCreatesTraceableOutput(){
        var recipe=recipe(new BigDecimal("6"));var order=service.createOrder(new OrderInput(recipe.id(),new BigDecimal("500"),LocalDate.now(),null));
        assertThat(order.requirements().getFirst().required()).isEqualByComparingTo("30");
        var started=service.start(order.id());assertThat(started.status()).isEqualTo(ProductionStatus.IN_PROGRESS);
        assertThat(started.consumptions()).extracting(c->c.batchCode()).containsExactly("LOTE-CEDO","LOTE-TARDE");
        assertThat(started.consumptions()).extracting(c->c.quantity()).containsExactly(new BigDecimal("10.000"),new BigDecimal("20.000"));
        var completed=service.complete(order.id(),new CompleteInput(new BigDecimal("485"),location,"Perda de modelagem",null));
        assertThat(completed.status()).isEqualTo(ProductionStatus.COMPLETED);assertThat(completed.output().batchCode()).startsWith("PROD-");
        assertThat(jdbc.queryForObject("SELECT quantity FROM stock_balances WHERE batch_id=?",BigDecimal.class,completed.output().batchId())).isEqualByComparingTo("485");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_id=?",Integer.class,order.id())).isEqualTo(2);
        assertThatThrownBy(()->service.complete(order.id(),new CompleteInput(BigDecimal.ONE,location,null,null))).isInstanceOf(DomainException.class);
    }
    @Test void insufficientIngredientRollsBackWithoutAnyConsumption(){
        var recipe=recipe(new BigDecimal("20"));var order=service.createOrder(new OrderInput(recipe.id(),new BigDecimal("500"),LocalDate.now(),null));
        assertThatThrownBy(()->service.start(order.id())).isInstanceOf(DomainException.class).hasMessage("INSUFFICIENT_STOCK");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM production_consumptions WHERE production_order_id=?",Integer.class,order.id())).isZero();
        assertThat(service.getOrder(order.id()).status()).isEqualTo(ProductionStatus.PLANNED);
    }
    @Test void allowsOnlyPlannedOrdersToBeCancelled(){var r=recipe(BigDecimal.ONE);var o=service.createOrder(new OrderInput(r.id(),BigDecimal.ONE,LocalDate.now(),null));assertThat(service.cancel(o.id()).status()).isEqualTo(ProductionStatus.CANCELLED);assertThatThrownBy(()->service.start(o.id())).isInstanceOf(DomainException.class);}
    @Test void rejectsZeroActualQuantityBeforeCreatingAnyOutput(){var r=recipe(BigDecimal.ONE);var o=service.createOrder(new OrderInput(r.id(),BigDecimal.ONE,LocalDate.now(),null));service.start(o.id());int batches=jdbc.queryForObject("SELECT COUNT(*) FROM batches",Integer.class),movements=jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements",Integer.class);assertThatThrownBy(()->service.complete(o.id(),new CompleteInput(BigDecimal.ZERO,location,null,null))).isInstanceOf(DomainException.class).hasMessage("INVALID_ACTUAL_QUANTITY");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM batches",Integer.class)).isEqualTo(batches);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements",Integer.class)).isEqualTo(movements);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM production_outputs WHERE production_order_id=?",Integer.class,o.id())).isZero();assertThat(service.getOrder(o.id()).status()).isEqualTo(ProductionStatus.IN_PROGRESS);}
    @Test void rejectsDuplicateIngredientsAndIncompatibleUnits(){assertThatThrownBy(()->service.createRecipe(new RecipeInput("Inválida",bread,BigDecimal.ONE,UnitOfMeasure.UNIT,null,true,null,List.of(new IngredientInput(flour,BigDecimal.ONE,UnitOfMeasure.L))))).isInstanceOf(DomainException.class).hasMessage("INCOMPATIBLE_UNITS");}
    @Test void concurrentStartsNeverCreateNegativeStock() throws Exception {
        jdbc.update("UPDATE stock_balances SET quantity=0 WHERE item_id=?",flour);jdbc.update("UPDATE stock_balances SET quantity=10 WHERE batch_id=?",early);
        var recipe=recipe(new BigDecimal("8"));var a=service.createOrder(new OrderInput(recipe.id(),new BigDecimal("100"),LocalDate.now(),null));var b=service.createOrder(new OrderInput(recipe.id(),new BigDecimal("100"),LocalDate.now(),null));
        ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
        Callable<Boolean> first=()->{ready.countDown();go.await();try{service.start(a.id());return true;}catch(DomainException e){return false;}};
        Callable<Boolean> second=()->{ready.countDown();go.await();try{service.start(b.id());return true;}catch(DomainException e){return false;}};
        Future<Boolean> one=executor.submit(first),two=executor.submit(second);ready.await();go.countDown();
        assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(true,false);executor.shutdown();
        assertThat(jdbc.queryForObject("SELECT quantity FROM stock_balances WHERE batch_id=?",BigDecimal.class,early)).isEqualByComparingTo("2");
    }
    private RecipeView recipe(BigDecimal ingredient){return service.createRecipe(new RecipeInput("Pão",bread,new BigDecimal("100"),UnitOfMeasure.UNIT,1,true,null,List.of(new IngredientInput(flour,ingredient,UnitOfMeasure.KG))));}
    private void batch(UUID id,String code,LocalDate expiration,Instant created){jdbc.update("INSERT INTO batches(id,item_id,code,manufacturing_date,expiration_date,active,created_at,updated_at) VALUES(?,?,?,CURRENT_DATE,?,true,?,?)",id,flour,code,expiration,created,created);}
    private void balance(UUID batch,BigDecimal quantity){jdbc.update("INSERT INTO stock_balances(id,item_id,batch_id,location_id,quantity,version,updated_at) VALUES(?,?,?,?,?,0,?)",UUID.randomUUID(),flour,batch,location,quantity,Instant.now());}
}
