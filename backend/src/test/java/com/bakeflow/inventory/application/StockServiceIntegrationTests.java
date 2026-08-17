package com.bakeflow.inventory.application;

import static com.bakeflow.inventory.application.StockDtos.*;
import static org.assertj.core.api.Assertions.*;

import com.bakeflow.inventory.domain.DomainException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StockServiceIntegrationTests {
    @Autowired StockService service;
    @Autowired JdbcTemplate jdbc;
    UUID item = UUID.randomUUID(), batch = UUID.randomUUID(), source = UUID.randomUUID();
    UUID destination = UUID.randomUUID(), actor = UUID.randomUUID();

    @BeforeEach void setup() {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO users(id,name,email,password_hash,active,created_at,updated_at) VALUES(?,?,?,? ,true,?,?)",
                actor, "Stock Operator", actor + "@test.local", "test-hash", now, now);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                actor.toString(), null, new SimpleGrantedAuthority("SCOPE_STOCK_ENTRY")));
        jdbc.update("INSERT INTO items(id,name,sku,type,unit,active,created_at,updated_at) VALUES(?,?,?,'RAW_MATERIAL','KG',true,?,?)",
                item, "Flour", "SKU-" + item, now, now);
        jdbc.update("INSERT INTO batches(id,item_id,code,expiration_date,active,created_at,updated_at) VALUES(?,?,?, ?,true,?,?)",
                batch, item, "LOT-" + batch, LocalDate.now().plusMonths(1), now, now);
        location(source, "SOURCE", now); location(destination, "DESTINATION", now);
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void keepsBalancesConsistentAndCreatesTraceableHistory() {
        service.entry(new EntryRequest(item, batch, source, new BigDecimal("100"), EntryReason.RECEIPT, "invoice"));
        service.transfer(new TransferRequest(item, batch, source, destination, new BigDecimal("30"), "replenishment"));
        service.exit(new ExitRequest(item, batch, source, new BigDecimal("10"), ExitReason.INTERNAL_USE, null));
        service.loss(new LossRequest(item, batch, destination, new BigDecimal("5"), LossReason.DAMAGED, "damaged bag"));
        service.adjustment(new AdjustmentRequest(item, batch, destination, new BigDecimal("27"), "physical count"));

        assertThat(quantity(source)).isEqualByComparingTo("60");
        assertThat(quantity(destination)).isEqualByComparingTo("27");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE item_id=?", Integer.class, item)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE item_id=? AND actor_user_id=?", Integer.class, item, actor)).isEqualTo(5);
        assertThat(service.movements(null, null, null, item, null, null, actor, 0, 20).content()).hasSize(5);
        assertThat(service.balances(item, null, null, null, null, "quantity", "desc", 0, 20).content()).hasSize(2);
    }

    @Test void rejectsInvalidOperationsWithoutChangingStock() {
        service.entry(new EntryRequest(item, batch, source, BigDecimal.TEN, EntryReason.RECEIPT, null));
        assertThatThrownBy(() -> service.exit(new ExitRequest(item, batch, source, new BigDecimal("11"), ExitReason.OTHER, null)))
                .isInstanceOf(DomainException.class).hasMessage("INSUFFICIENT_STOCK");
        assertThatThrownBy(() -> service.transfer(new TransferRequest(item, batch, source, source, BigDecimal.ONE, null)))
                .isInstanceOf(DomainException.class).hasMessage("SAME_TRANSFER_LOCATION");
        assertThatThrownBy(() -> service.loss(new LossRequest(item, batch, source, BigDecimal.ONE, LossReason.OTHER, " ")))
                .isInstanceOf(DomainException.class).hasMessage("LOSS_DESCRIPTION_REQUIRED");
        assertThatThrownBy(() -> service.adjustment(new AdjustmentRequest(item, batch, source, BigDecimal.TEN, "count")))
                .isInstanceOf(DomainException.class).hasMessage("NO_STOCK_DIFFERENCE");
        assertThat(quantity(source)).isEqualByComparingTo("10");
    }

    @Test void rejectsInactiveAndMismatchedCatalogReferences() {
        UUID other = UUID.randomUUID(); Instant now = Instant.now();
        jdbc.update("INSERT INTO items(id,name,type,unit,active,created_at,updated_at) VALUES(?,?,'RAW_MATERIAL','KG',true,?,?)", other, "Other", now, now);
        assertThatThrownBy(() -> service.entry(new EntryRequest(other, batch, source, BigDecimal.ONE, EntryReason.OTHER, null)))
                .isInstanceOf(DomainException.class).hasMessage("LOT_ITEM_MISMATCH");
        jdbc.update("UPDATE items SET active=false WHERE id=?", item);
        assertThatThrownBy(() -> service.entry(new EntryRequest(item, batch, source, BigDecimal.ONE, EntryReason.OTHER, null)))
                .isInstanceOf(DomainException.class).hasMessage("INACTIVE_ITEM");
    }

    @Test void concurrentExitsNeverProduceNegativeStock() throws Exception {
        service.entry(new EntryRequest(item, batch, source, BigDecimal.TEN, EntryReason.RECEIPT, null));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        Callable<Boolean> takeEight = () -> exitConcurrently(new BigDecimal("8"), ready, go);
        Callable<Boolean> takeFive = () -> exitConcurrently(new BigDecimal("5"), ready, go);
        Future<Boolean> first = executor.submit(takeEight), second = executor.submit(takeFive);
        ready.await(); go.countDown();
        assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        executor.shutdown();
        assertThat(quantity(source)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    private boolean exitConcurrently(BigDecimal quantity, CountDownLatch ready, CountDownLatch go)
            throws InterruptedException {
        ready.countDown(); go.await();
        try {
            service.exit(new ExitRequest(item, batch, source, quantity, ExitReason.INTERNAL_USE, null));
            return true;
        } catch (DomainException exception) {
            return false;
        }
    }

    private void location(UUID id, String code, Instant now) {
        jdbc.update("INSERT INTO locations(id,name,code,type,active,created_at,updated_at) VALUES(?,?,?,'WAREHOUSE',true,?,?)", id, code, code + id, now, now);
    }
    private BigDecimal quantity(UUID location) {
        return jdbc.queryForObject("SELECT quantity FROM stock_balances WHERE batch_id=? AND location_id=?", BigDecimal.class, batch, location);
    }
}
