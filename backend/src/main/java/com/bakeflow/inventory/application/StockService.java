package com.bakeflow.inventory.application;

import static com.bakeflow.inventory.application.StockDtos.*;

import com.bakeflow.audit.AuditService;
import com.bakeflow.identity.SecuritySupport;
import com.bakeflow.inventory.domain.DomainException;
import com.bakeflow.inventory.domain.UnitOfMeasure;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {
    private static final int MAX_PAGE_SIZE = 100;
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public StockService(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public BalancePage balances(UUID itemId, String sku, String batch, UUID locationId,
            ExpirationStatus expiration, String sort, String direction, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<Object> args = new ArrayList<>();
        String where = balanceWhere(itemId, sku, batch, locationId, expiration, args);
        String order = balanceOrder(sort, direction);
        long total = count("SELECT COUNT(*) FROM stock_balances s JOIN items i ON i.id=s.item_id "
                + "JOIN batches b ON b.id=s.batch_id JOIN locations l ON l.id=s.location_id " + where, args);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add(safePage * safeSize);
        List<BalanceView> content = jdbc.query("""
                SELECT s.id,s.item_id,i.name item_name,i.sku,s.batch_id,b.code,b.expiration_date,
                       s.location_id,l.name location_name,s.quantity,i.unit,s.updated_at
                FROM stock_balances s JOIN items i ON i.id=s.item_id
                JOIN batches b ON b.id=s.batch_id JOIN locations l ON l.id=s.location_id
                """ + where + order + " LIMIT ? OFFSET ?", this::balance, pageArgs.toArray());
        return new BalancePage(content, safePage, safeSize, total,
                (int) Math.ceil((double) total / safeSize));
    }

    @Transactional(readOnly = true)
    public MovementPage movements(LocalDate startDate, LocalDate endDate, MovementType type,
            UUID itemId, UUID batchId, UUID locationId, UUID actorUserId, int page, int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new DomainException("INVALID_DATE_RANGE");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<Object> args = new ArrayList<>();
        String where = movementWhere(startDate, endDate, type, itemId, batchId, locationId,
                actorUserId, args);
        String joins = " FROM stock_movements m JOIN items i ON i.id=m.item_id "
                + "JOIN batches b ON b.id=m.batch_id JOIN locations current_location ON current_location.id=m.location_id "
                + "LEFT JOIN locations source ON source.id=m.source_location_id "
                + "LEFT JOIN locations destination ON destination.id=m.destination_location_id "
                + "LEFT JOIN users actor ON actor.id=m.actor_user_id ";
        long total = count("SELECT COUNT(*)" + joins + where, args);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add(safePage * safeSize);
        List<MovementView> content = jdbc.query("SELECT m.*,i.name item_name,i.sku,i.unit,b.code batch_code,"
                + "source.name source_name,destination.name destination_name,actor.name actor_name"
                + joins + where + " ORDER BY m.created_at DESC,m.id DESC LIMIT ? OFFSET ?",
                this::mapMovement, pageArgs.toArray());
        return new MovementPage(content, safePage, safeSize, total,
                (int) Math.ceil((double) total / safeSize));
    }

    @Transactional(readOnly = true)
    public MovementView movement(UUID id) {
        List<MovementView> values = jdbc.query("""
                SELECT m.*,i.name item_name,i.sku,i.unit,b.code batch_code,
                       source.name source_name,destination.name destination_name,actor.name actor_name
                FROM stock_movements m JOIN items i ON i.id=m.item_id
                JOIN batches b ON b.id=m.batch_id
                LEFT JOIN locations source ON source.id=m.source_location_id
                LEFT JOIN locations destination ON destination.id=m.destination_location_id
                LEFT JOIN users actor ON actor.id=m.actor_user_id WHERE m.id=?
                """, this::mapMovement, id);
        if (values.isEmpty()) throw new DomainException("STOCK_MOVEMENT_NOT_FOUND");
        return values.getFirst();
    }

    public MovementView entry(EntryRequest request) {
        Context context = validateContext(request.itemId(), request.batchId(), request.locationId());
        BigDecimal quantity = positive(request.quantity());
        BigDecimal previous = lockBalance(request.batchId(), request.locationId());
        BigDecimal resulting = previous.add(quantity);
        writeBalance(context, request.locationId(), previous, resulting);
        return record(MovementType.ENTRY, context, null, request.locationId(), quantity,
                request.reason().name(), clean(request.notes()), previous, resulting, null);
    }

    public MovementView exit(ExitRequest request) {
        Context context = validateContext(request.itemId(), request.batchId(), request.locationId());
        BigDecimal quantity = positive(request.quantity());
        BigDecimal previous = lockBalance(request.batchId(), request.locationId());
        BigDecimal resulting = subtract(previous, quantity);
        writeBalance(context, request.locationId(), previous, resulting);
        return record(MovementType.EXIT, context, request.locationId(), null, quantity,
                request.reason().name(), clean(request.notes()), previous, resulting, null);
    }

    public MovementView transfer(TransferRequest request) {
        if (request.sourceLocationId().equals(request.destinationLocationId())) {
            throw new DomainException("SAME_TRANSFER_LOCATION");
        }
        Context context = validateContext(request.itemId(), request.batchId(),
                request.sourceLocationId());
        validateActiveLocation(request.destinationLocationId());
        Map<UUID, BigDecimal> locked = lockBalances(request.batchId(), request.sourceLocationId(),
                request.destinationLocationId());
        BigDecimal quantity = positive(request.quantity());
        BigDecimal sourcePrevious = locked.getOrDefault(request.sourceLocationId(), BigDecimal.ZERO);
        BigDecimal destinationPrevious = locked.getOrDefault(request.destinationLocationId(), BigDecimal.ZERO);
        writeBalance(context, request.sourceLocationId(), sourcePrevious,
                subtract(sourcePrevious, quantity));
        writeBalance(context, request.destinationLocationId(), destinationPrevious,
                destinationPrevious.add(quantity));
        return record(MovementType.TRANSFER, context, request.sourceLocationId(),
                request.destinationLocationId(), quantity, "TRANSFER", clean(request.notes()),
                sourcePrevious, sourcePrevious.subtract(quantity), null);
    }

    public MovementView loss(LossRequest request) {
        if (request.reason() == LossReason.OTHER && clean(request.notes()) == null) {
            throw new DomainException("LOSS_DESCRIPTION_REQUIRED");
        }
        Context context = validateContext(request.itemId(), request.batchId(), request.locationId());
        BigDecimal quantity = positive(request.quantity());
        BigDecimal previous = lockBalance(request.batchId(), request.locationId());
        BigDecimal resulting = subtract(previous, quantity);
        writeBalance(context, request.locationId(), previous, resulting);
        return record(MovementType.LOSS, context, request.locationId(), null, quantity,
                request.reason().name(), clean(request.notes()), previous, resulting, null);
    }

    public MovementView adjustment(AdjustmentRequest request) {
        Context context = validateContext(request.itemId(), request.batchId(), request.locationId());
        BigDecimal physical = nonNegative(request.physicalQuantity());
        BigDecimal previous = lockBalance(request.batchId(), request.locationId());
        BigDecimal difference = physical.subtract(previous);
        if (difference.signum() == 0) throw new DomainException("NO_STOCK_DIFFERENCE");
        writeBalance(context, request.locationId(), previous, physical);
        UUID source = difference.signum() < 0 ? request.locationId() : null;
        UUID destination = difference.signum() > 0 ? request.locationId() : null;
        return record(MovementType.ADJUSTMENT, context, source, destination, difference.abs(),
                "INVENTORY_COUNT", request.justification().trim(), previous, physical, null);
    }

    private Context validateContext(UUID itemId, UUID batchId, UUID locationId) {
        List<Context> contexts = jdbc.query("""
                SELECT i.id item_id,i.name,i.unit,i.active item_active,b.id batch_id,b.code,
                       b.active batch_active,b.item_id batch_item_id
                FROM items i JOIN batches b ON b.item_id=i.id WHERE i.id=? AND b.id=? FOR UPDATE OF b
                """, (rs, row) -> new Context(rs.getObject("item_id", UUID.class), rs.getString("name"),
                UnitOfMeasure.valueOf(rs.getString("unit")), rs.getBoolean("item_active"),
                rs.getObject("batch_id", UUID.class), rs.getString("code"),
                rs.getBoolean("batch_active")), itemId, batchId);
        if (contexts.isEmpty()) {
            Integer batchExists = jdbc.queryForObject("SELECT COUNT(*) FROM batches WHERE id=?",
                    Integer.class, batchId);
            throw new DomainException(batchExists != null && batchExists > 0
                    ? "LOT_ITEM_MISMATCH" : "BATCH_NOT_FOUND");
        }
        Context context = contexts.getFirst();
        if (!context.itemActive()) throw new DomainException("INACTIVE_ITEM");
        if (!context.batchActive()) throw new DomainException("INACTIVE_BATCH");
        validateActiveLocation(locationId);
        return context;
    }

    private void validateActiveLocation(UUID id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM locations WHERE id=? AND active=true",
                Integer.class, id);
        if (count == null || count == 0) throw new DomainException("INVALID_STOCK_LOCATION");
    }

    private BigDecimal lockBalance(UUID batchId, UUID locationId) {
        List<BigDecimal> values = jdbc.query("SELECT quantity FROM stock_balances "
                + "WHERE batch_id=? AND location_id=? FOR UPDATE", (rs, row) -> rs.getBigDecimal(1),
                batchId, locationId);
        return values.isEmpty() ? BigDecimal.ZERO : values.getFirst();
    }

    private Map<UUID, BigDecimal> lockBalances(UUID batchId, UUID source, UUID destination) {
        Map<UUID, BigDecimal> values = new HashMap<>();
        jdbc.query("SELECT location_id,quantity FROM stock_balances WHERE batch_id=? "
                + "AND location_id IN (?,?) ORDER BY location_id FOR UPDATE", rs -> {
                    values.put(rs.getObject(1, UUID.class), rs.getBigDecimal(2));
                }, batchId, source, destination);
        return values;
    }

    private void writeBalance(Context context, UUID locationId, BigDecimal previous,
            BigDecimal resulting) {
        Timestamp now = Timestamp.from(Instant.now());
        int changed = jdbc.update("UPDATE stock_balances SET quantity=?,version=version+1,updated_at=? "
                + "WHERE batch_id=? AND location_id=?", resulting, now, context.batchId(), locationId);
        if (changed == 0) {
            jdbc.update("INSERT INTO stock_balances(id,item_id,batch_id,location_id,quantity,version,updated_at) "
                    + "VALUES(?,?,?,?,?,0,?)", UUID.randomUUID(), context.itemId(), context.batchId(),
                    locationId, resulting, now);
        }
    }

    private MovementView record(MovementType type, Context context, UUID source, UUID destination,
            BigDecimal quantity, String reason, String notes, BigDecimal previous,
            BigDecimal resulting, String reference) {
        UUID id = UUID.randomUUID();
        UUID actor = SecuritySupport.currentUserId();
        UUID compatibilityLocation = source != null ? source : destination;
        jdbc.update("""
                INSERT INTO stock_movements(id,item_id,batch_id,location_id,type,quantity,reference,
                  source_location_id,destination_location_id,actor_user_id,reason,notes,
                  previous_quantity,resulting_quantity,created_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, context.itemId(), context.batchId(), compatibilityLocation, type.name(),
                quantity, reference, source, destination, actor, reason, notes, previous, resulting,
                Timestamp.from(Instant.now()));
        audit.record("STOCK_" + type.name() + "_CREATED", "STOCK_MOVEMENT", id,
                "Stock operation recorded", Map.of("itemId", context.itemId().toString(),
                        "batchId", context.batchId().toString(), "quantity", quantity.toPlainString()));
        return movement(id);
    }

    private String balanceWhere(UUID itemId, String sku, String batch, UUID locationId,
            ExpirationStatus expiration, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE s.quantity>0");
        if (itemId != null) { where.append(" AND s.item_id=?"); args.add(itemId); }
        if (text(sku) != null) { where.append(" AND LOWER(i.sku) LIKE ?"); args.add("%" + text(sku) + "%"); }
        if (text(batch) != null) { where.append(" AND LOWER(b.code) LIKE ?"); args.add("%" + text(batch) + "%"); }
        if (locationId != null) { where.append(" AND s.location_id=?"); args.add(locationId); }
        if (expiration != null) switch (expiration) {
            case EXPIRED -> where.append(" AND b.expiration_date<CURRENT_DATE");
            case EXPIRING_SOON -> where.append(" AND b.expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE+30");
            case VALID -> where.append(" AND (b.expiration_date IS NULL OR b.expiration_date>=CURRENT_DATE)");
            case WITHOUT_EXPIRATION -> where.append(" AND b.expiration_date IS NULL");
        }
        return where.toString();
    }

    private String balanceOrder(String sort, String direction) {
        String column = switch (sort == null ? "item" : sort) {
            case "quantity" -> "s.quantity";
            case "expiration" -> "b.expiration_date";
            case "updatedAt" -> "s.updated_at";
            default -> "i.name";
        };
        String order = "desc".equalsIgnoreCase(direction) ? " DESC" : " ASC";
        return " ORDER BY " + column + order + ",s.id";
    }

    private String movementWhere(LocalDate start, LocalDate end, MovementType type, UUID itemId,
            UUID batchId, UUID locationId, UUID actor, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (start != null) { where.append(" AND m.created_at>=?"); args.add(Timestamp.from(start.atStartOfDay().toInstant(ZoneOffset.UTC))); }
        if (end != null) { where.append(" AND m.created_at<?"); args.add(Timestamp.from(end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))); }
        if (type != null) { where.append(" AND m.type=?"); args.add(type.name()); }
        if (itemId != null) { where.append(" AND m.item_id=?"); args.add(itemId); }
        if (batchId != null) { where.append(" AND m.batch_id=?"); args.add(batchId); }
        if (locationId != null) { where.append(" AND (m.source_location_id=? OR m.destination_location_id=?)"); args.add(locationId); args.add(locationId); }
        if (actor != null) { where.append(" AND m.actor_user_id=?"); args.add(actor); }
        return where.toString();
    }

    private BalanceView balance(ResultSet rs, int row) throws SQLException {
        LocalDate expiration = rs.getObject("expiration_date", LocalDate.class);
        ExpirationStatus status = expiration == null ? ExpirationStatus.WITHOUT_EXPIRATION
                : expiration.isBefore(LocalDate.now(ZoneOffset.UTC)) ? ExpirationStatus.EXPIRED
                : !expiration.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(30))
                        ? ExpirationStatus.EXPIRING_SOON : ExpirationStatus.VALID;
        return new BalanceView(rs.getObject("id", UUID.class), rs.getObject("item_id", UUID.class),
                rs.getString("item_name"), rs.getString("sku"), rs.getObject("batch_id", UUID.class),
                rs.getString("code"), expiration, status, rs.getObject("location_id", UUID.class),
                rs.getString("location_name"), rs.getBigDecimal("quantity"),
                UnitOfMeasure.valueOf(rs.getString("unit")), rs.getTimestamp("updated_at").toInstant());
    }

    private MovementView mapMovement(ResultSet rs, int row) throws SQLException {
        return new MovementView(rs.getObject("id", UUID.class), MovementType.valueOf(rs.getString("type")),
                rs.getObject("item_id", UUID.class), rs.getString("item_name"), rs.getString("sku"),
                rs.getObject("batch_id", UUID.class), rs.getString("batch_code"),
                rs.getObject("source_location_id", UUID.class), rs.getString("source_name"),
                rs.getObject("destination_location_id", UUID.class), rs.getString("destination_name"),
                rs.getBigDecimal("quantity"), UnitOfMeasure.valueOf(rs.getString("unit")),
                rs.getObject("actor_user_id", UUID.class), rs.getString("actor_name"),
                rs.getString("reason"), rs.getString("notes"), rs.getString("reference"),
                rs.getBigDecimal("previous_quantity"), rs.getBigDecimal("resulting_quantity"),
                rs.getTimestamp("created_at").toInstant());
    }

    private long count(String sql, List<Object> args) {
        Long value = jdbc.queryForObject(sql, Long.class, args.toArray());
        return value == null ? 0 : value;
    }

    private BigDecimal subtract(BigDecimal available, BigDecimal requested) {
        if (available.compareTo(requested) < 0) throw new DomainException("INSUFFICIENT_STOCK");
        return available.subtract(requested);
    }
    private BigDecimal positive(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new DomainException("INVALID_QUANTITY");
        return value;
    }
    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new DomainException("INVALID_QUANTITY");
        return value;
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String text(String value) { return value == null || value.isBlank() ? null : value.trim().toLowerCase(); }

    private record Context(UUID itemId, String itemName, UnitOfMeasure unit, boolean itemActive,
            UUID batchId, String batchCode, boolean batchActive) {}
}
