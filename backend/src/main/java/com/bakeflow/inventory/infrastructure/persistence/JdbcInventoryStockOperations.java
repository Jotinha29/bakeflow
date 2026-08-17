package com.bakeflow.inventory.infrastructure.persistence;

import com.bakeflow.inventory.application.InventoryStockOperations;
import com.bakeflow.audit.AuditService;
import com.bakeflow.inventory.domain.DomainException;
import com.bakeflow.identity.SecuritySupport;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcInventoryStockOperations implements InventoryStockOperations {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    public JdbcInventoryStockOperations(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }

    @Override public List<Availability> preview(List<Requirement> requirements) {
        return requirements.stream().map(requirement -> availability(requirement, false)).toList();
    }

    @Override public List<Allocation> consumeFefo(List<Requirement> requirements, String reference) {
        List<Availability> availability = requirements.stream().map(r -> availability(r, true)).toList();
        if (availability.stream().anyMatch(a -> !a.sufficient())) throw new DomainException("INSUFFICIENT_STOCK");
        List<Allocation> consumed = availability.stream().flatMap(a -> a.allocations().stream()).toList();
        consumed.forEach(allocation -> {
            int changed = jdbc.update("UPDATE stock_balances SET quantity=quantity-?, version=version+1, updated_at=? WHERE batch_id=? AND location_id=? AND quantity>=?",
                allocation.quantity(), Timestamp.from(Instant.now()), allocation.batchId(), allocation.locationId(), allocation.quantity());
            if (changed != 1) throw new DomainException("CONCURRENT_MODIFICATION");
            movement(allocation, "PRODUCTION_CONSUMPTION", reference);
            audit.record("STOCK_EXIT_CREATED", "STOCK_MOVEMENT", allocation.itemId(), "Stock consumed by production", java.util.Map.of("reference", reference, "quantity", allocation.quantity().toPlainString()));
        });
        return consumed;
    }

    @Override public void receive(UUID itemId, UUID batchId, UUID locationId, BigDecimal quantity, String reference) {
        if (quantity == null || quantity.signum() <= 0) throw new DomainException("INVALID_QUANTITY");
        int changed = jdbc.update("UPDATE stock_balances SET quantity=quantity+?, version=version+1, updated_at=? WHERE batch_id=? AND location_id=?",
            quantity, Timestamp.from(Instant.now()), batchId, locationId);
        if (changed == 0) jdbc.update("INSERT INTO stock_balances(id,item_id,batch_id,location_id,quantity,version,updated_at) VALUES(?,?,?,?,?,0,?)",
            UUID.randomUUID(), itemId, batchId, locationId, quantity, Timestamp.from(Instant.now()));
        movement(new Allocation(itemId, "", batchId, "", locationId, "", null, quantity), "PRODUCTION_OUTPUT", reference);
        audit.record("STOCK_ENTRY_CREATED", "STOCK_MOVEMENT", itemId, "Finished product received into stock", java.util.Map.of("reference", reference, "quantity", quantity.toPlainString()));
    }

    private Availability availability(Requirement requirement, boolean lock) {
        String sql = """
            SELECT s.item_id,i.name,s.batch_id,b.code,s.location_id,l.name,b.expiration_date,s.quantity
            FROM stock_balances s JOIN items i ON i.id=s.item_id JOIN batches b ON b.id=s.batch_id
            JOIN locations l ON l.id=s.location_id
            WHERE s.item_id=? AND s.quantity>0 AND i.active=true AND b.active=true AND l.active=true
              AND (b.expiration_date IS NULL OR b.expiration_date>=CURRENT_DATE)
            ORDER BY CASE WHEN b.expiration_date IS NULL THEN 1 ELSE 0 END, b.expiration_date, b.created_at, b.id
            """ + (lock ? " FOR UPDATE OF s" : "");
        List<Allocation> balances = jdbc.query(sql, this::allocation, requirement.itemId());
        BigDecimal remaining = requirement.quantity();
        List<Allocation> selected = new ArrayList<>();
        for (Allocation balance : balances) {
            if (remaining.signum() <= 0) break;
            BigDecimal take = balance.quantity().min(remaining);
            selected.add(new Allocation(balance.itemId(), balance.itemName(), balance.batchId(), balance.batchCode(),
                balance.locationId(), balance.locationName(), balance.expirationDate(), take));
            remaining = remaining.subtract(take);
        }
        BigDecimal available = balances.stream().map(Allocation::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        String name = balances.isEmpty() ? jdbc.queryForObject("SELECT name FROM items WHERE id=?", String.class, requirement.itemId()) : balances.getFirst().itemName();
        return new Availability(requirement.itemId(), name, requirement.quantity(), available,
            available.compareTo(requirement.quantity()) >= 0, selected);
    }
    private Allocation allocation(ResultSet rs, int row) throws SQLException {
        return new Allocation(rs.getObject(1, UUID.class), rs.getString(2), rs.getObject(3, UUID.class), rs.getString(4),
            rs.getObject(5, UUID.class), rs.getString(6), rs.getObject(7, LocalDate.class), rs.getBigDecimal(8));
    }
    private void movement(Allocation a, String type, String reference) {
        UUID source = "PRODUCTION_CONSUMPTION".equals(type) ? a.locationId() : null;
        UUID destination = "PRODUCTION_OUTPUT".equals(type) ? a.locationId() : null;
        jdbc.update("""
                INSERT INTO stock_movements(id,item_id,batch_id,location_id,type,quantity,reference,
                  source_location_id,destination_location_id,actor_user_id,reason,created_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), a.itemId(), a.batchId(), a.locationId(), type, a.quantity(),
                reference, source, destination, SecuritySupport.currentUserId(), "PRODUCTION",
                Timestamp.from(Instant.now()));
    }
}
