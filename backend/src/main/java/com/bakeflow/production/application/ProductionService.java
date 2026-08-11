package com.bakeflow.production.application;

import static com.bakeflow.production.application.ProductionDtos.*;

import com.bakeflow.inventory.application.InventoryStockOperations;
import com.bakeflow.inventory.application.InventoryStockOperations.Requirement;
import com.bakeflow.inventory.domain.DomainException;
import com.bakeflow.inventory.domain.UnitOfMeasure;
import com.bakeflow.production.domain.ProductionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class ProductionService {
    private static final Logger log = LoggerFactory.getLogger(ProductionService.class);
    private final JdbcTemplate jdbc;
    private final InventoryStockOperations stock;
    public ProductionService(JdbcTemplate jdbc, InventoryStockOperations stock) { this.jdbc = jdbc; this.stock = stock; }

    public RecipeView createRecipe(RecipeInput input) { return saveRecipe(UUID.randomUUID(), input, true); }
    public RecipeView updateRecipe(UUID id, RecipeInput input) { requireExists("recipes", id, "RECIPE_NOT_FOUND"); return saveRecipe(id, input, false); }
    public void setRecipeActive(UUID id, boolean active) { requireExists("recipes", id, "RECIPE_NOT_FOUND"); jdbc.update("UPDATE recipes SET active=?,updated_at=? WHERE id=?", active, Timestamp.from(Instant.now()), id); }
    @Transactional(readOnly=true) public RecipeView recipe(UUID id) { return recipeRow(id); }
    @Transactional(readOnly=true) public List<RecipeView> recipes(String search, UUID outputItemId, Boolean active) {
        StringBuilder sql=new StringBuilder("SELECT id FROM recipes WHERE 1=1");List<Object> args=new ArrayList<>();
        if(search!=null&&!search.isBlank()){sql.append(" AND LOWER(name) LIKE ?");args.add("%"+search.trim().toLowerCase()+"%");}
        if(outputItemId!=null){sql.append(" AND output_item_id=?");args.add(outputItemId);}if(active!=null){sql.append(" AND active=?");args.add(active);}sql.append(" ORDER BY name");
        List<UUID> ids=jdbc.query(sql.toString(),(rs,n)->rs.getObject(1,UUID.class),args.toArray());return ids.stream().map(this::recipeRow).toList();
    }
    public ProductionOrderView createOrder(OrderInput input) {
        if (input.recipeId()==null || positive(input.plannedQuantity())==null || input.plannedDate()==null) throw new DomainException("INVALID_PRODUCTION_ORDER");
        RecipeView recipe=recipeRow(input.recipeId()); if(!recipe.active()) throw new DomainException("INACTIVE_RECIPE");
        long number=jdbc.queryForObject("SELECT nextval('production_order_code_seq')",Long.class);
        UUID id=UUID.randomUUID(); Timestamp now=Timestamp.from(Instant.now()); String code="OP-"+LocalDate.now(ZoneOffset.UTC).getYear()+"-"+String.format("%06d",number);
        jdbc.update("INSERT INTO production_orders(id,code,recipe_id,planned_quantity,status,planned_date,notes,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",
            id,code,input.recipeId(),input.plannedQuantity(),ProductionStatus.PLANNED.name(),input.plannedDate(),clean(input.notes()),now,now);
        return order(id,true);
    }
    @Transactional(readOnly=true) public ProductionPreview previewPlan(PreviewInput input) {
        if(input.recipeId()==null||positive(input.plannedQuantity())==null)throw new DomainException("INVALID_PRODUCTION_ORDER");
        RecipeView recipe=recipeRow(input.recipeId());OrderBase base=new OrderBase(null,null,recipe.id(),recipe.name(),recipe.outputItemId(),recipe.outputItemName(),input.plannedQuantity(),null,recipe.yieldUnit(),ProductionStatus.PLANNED,LocalDate.now(),null,null,null,null,null,null);
        return new ProductionPreview(recipe,input.plannedQuantity(),requirementsView(base));
    }
    @Transactional(readOnly=true) public List<ProductionOrderView> orders(String code, UUID recipeId, ProductionStatus status, LocalDate plannedDate) {
        StringBuilder sql=new StringBuilder("SELECT id FROM production_orders WHERE 1=1");List<Object> args=new ArrayList<>();
        if(code!=null&&!code.isBlank()){sql.append(" AND LOWER(code) LIKE ?");args.add("%"+code.trim().toLowerCase()+"%");}
        if(recipeId!=null){sql.append(" AND recipe_id=?");args.add(recipeId);}if(status!=null){sql.append(" AND status=?");args.add(status.name());}if(plannedDate!=null){sql.append(" AND planned_date=?");args.add(plannedDate);}sql.append(" ORDER BY created_at DESC");
        List<UUID> ids=jdbc.query(sql.toString(),(rs,n)->rs.getObject(1,UUID.class),args.toArray());return ids.stream().map(id->order(id,false)).toList();
    }
    @Transactional(readOnly=true) public ProductionOrderView getOrder(UUID id) { return order(id,true); }
    @Transactional(readOnly=true) public ProductionOrderView preview(UUID id) { return order(id,true); }
    @Transactional(readOnly=true) public ProductionSummary summary() {
        long planned=jdbc.queryForObject("SELECT COUNT(*) FROM production_orders WHERE status='PLANNED'",Long.class);
        long inProgress=jdbc.queryForObject("SELECT COUNT(*) FROM production_orders WHERE status='IN_PROGRESS'",Long.class);
        long completed=jdbc.queryForObject("SELECT COUNT(*) FROM production_orders WHERE status='COMPLETED' AND CAST(completed_at AS DATE)=CURRENT_DATE",Long.class);
        List<UUID> ids=jdbc.query("SELECT id FROM production_orders ORDER BY updated_at DESC FETCH FIRST 5 ROWS ONLY",(rs,n)->rs.getObject(1,UUID.class));List<ProductionOrderView> recent=ids.stream().map(id->order(id,false)).toList();
        return new ProductionSummary(planned,inProgress,completed,recent);
    }

    public ProductionOrderView start(UUID id) {
        OrderBase base=lockOrder(id); if(base.status()!=ProductionStatus.PLANNED) throw new DomainException("INVALID_PRODUCTION_TRANSITION");
        List<Requirement> requirements=requirements(base.recipeId(),base.plannedQuantity());
        var consumed=stock.consumeFefo(requirements,base.code()); Timestamp now=Timestamp.from(Instant.now());
        consumed.forEach(a->jdbc.update("INSERT INTO production_consumptions(id,production_order_id,item_id,batch_id,location_id,quantity,created_at) VALUES(?,?,?,?,?,?,?)",
            UUID.randomUUID(),id,a.itemId(),a.batchId(),a.locationId(),a.quantity(),now));
        jdbc.update("UPDATE production_orders SET status='IN_PROGRESS',started_at=?,updated_at=? WHERE id=?",now,now,id);
        log.info("Production order started: {}",base.code()); return order(id,true);
    }
    public ProductionOrderView complete(UUID id, CompleteInput input) {
        OrderBase base=lockOrder(id); if(base.status()!=ProductionStatus.IN_PROGRESS) throw new DomainException("INVALID_PRODUCTION_TRANSITION");
        BigDecimal actual=nonNegative(input.actualQuantity()); if(input.destinationLocationId()==null) throw new DomainException("DESTINATION_REQUIRED");
        RecipeView recipe=recipeRow(base.recipeId()); validateActiveLocation(input.destinationLocationId());
        UUID batchId=UUID.randomUUID(); Timestamp now=Timestamp.from(Instant.now()); LocalDate productionDate=LocalDate.now(ZoneOffset.UTC);
        LocalDate expiration=recipe.shelfLifeDays()==null?null:productionDate.plusDays(recipe.shelfLifeDays());
        String batchCode="PROD-"+productionDate.format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+base.code().substring(base.code().length()-6);
        jdbc.update("INSERT INTO batches(id,item_id,code,manufacturing_date,expiration_date,active,created_at,updated_at) VALUES(?,?,?,?,?,true,?,?)",
            batchId,recipe.outputItemId(),batchCode,productionDate,expiration,now,now);
        stock.receive(recipe.outputItemId(),batchId,input.destinationLocationId(),actual,base.code());
        jdbc.update("INSERT INTO production_outputs(id,production_order_id,item_id,batch_id,location_id,quantity,created_at) VALUES(?,?,?,?,?,?,?)",
            UUID.randomUUID(),id,recipe.outputItemId(),batchId,input.destinationLocationId(),actual,now);
        jdbc.update("UPDATE production_orders SET status='COMPLETED',actual_quantity=?,destination_location_id=?,difference_reason=?,notes=COALESCE(?,notes),completed_at=?,updated_at=? WHERE id=?",
            actual,input.destinationLocationId(),clean(input.differenceReason()),clean(input.notes()),now,now,id);
        log.info("Production order completed: {}",base.code()); return order(id,true);
    }
    public ProductionOrderView cancel(UUID id) {
        OrderBase base=lockOrder(id); if(base.status()!=ProductionStatus.PLANNED) throw new DomainException("INVALID_PRODUCTION_TRANSITION");
        jdbc.update("UPDATE production_orders SET status='CANCELLED',updated_at=? WHERE id=?",Timestamp.from(Instant.now()),id);
        log.info("Production order cancelled: {}",base.code()); return order(id,true);
    }

    private RecipeView saveRecipe(UUID id, RecipeInput input, boolean insert) {
        validateRecipe(input); Timestamp now=Timestamp.from(Instant.now());
        if(insert) jdbc.update("INSERT INTO recipes(id,name,output_item_id,yield_quantity,yield_unit,shelf_life_days,active,notes,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
            id,input.name().trim(),input.outputItemId(),input.yieldQuantity(),input.yieldUnit().name(),input.shelfLifeDays(),input.active(),clean(input.notes()),now,now);
        else { jdbc.update("UPDATE recipes SET name=?,output_item_id=?,yield_quantity=?,yield_unit=?,shelf_life_days=?,active=?,notes=?,updated_at=? WHERE id=?",
            input.name().trim(),input.outputItemId(),input.yieldQuantity(),input.yieldUnit().name(),input.shelfLifeDays(),input.active(),clean(input.notes()),now,id); jdbc.update("DELETE FROM recipe_ingredients WHERE recipe_id=?",id); }
        input.ingredients().forEach(i->jdbc.update("INSERT INTO recipe_ingredients(id,recipe_id,item_id,quantity,unit) VALUES(?,?,?,?,?)",UUID.randomUUID(),id,i.itemId(),i.quantity(),i.unit().name()));
        return recipeRow(id);
    }
    private void validateRecipe(RecipeInput input) {
        if(input.name()==null||input.name().isBlank()||input.outputItemId()==null||positive(input.yieldQuantity())==null||input.yieldUnit()==null||input.ingredients()==null||input.ingredients().isEmpty()) throw new DomainException("INVALID_RECIPE");
        String outputType=jdbc.queryForObject("SELECT type FROM items WHERE id=? AND active=true",String.class,input.outputItemId());
        if(!"FINISHED_PRODUCT".equals(outputType)) throw new DomainException("INVALID_OUTPUT_ITEM");
        Set<UUID> unique=new HashSet<>(); for(IngredientInput ingredient:input.ingredients()) {
            if(ingredient.itemId()==null||positive(ingredient.quantity())==null||ingredient.unit()==null||!unique.add(ingredient.itemId())) throw new DomainException("INVALID_RECIPE_INGREDIENT");
            Map<String,Object> item=jdbc.queryForMap("SELECT type,unit,active FROM items WHERE id=?",ingredient.itemId());
            if(!Boolean.TRUE.equals(item.get("active"))||!(item.get("type").equals("RAW_MATERIAL")||item.get("type").equals("PACKAGING"))) throw new DomainException("INVALID_RECIPE_INGREDIENT");
            conversion(ingredient.quantity(),ingredient.unit(),UnitOfMeasure.valueOf(item.get("unit").toString()));
        }
    }
    private List<Requirement> requirements(UUID recipeId, BigDecimal planned) {
        RecipeView recipe=recipeRow(recipeId); BigDecimal factor=planned.divide(recipe.yieldQuantity(),9,RoundingMode.HALF_UP);
        return recipe.ingredients().stream().map(i->{ UnitOfMeasure itemUnit=UnitOfMeasure.valueOf(jdbc.queryForObject("SELECT unit FROM items WHERE id=?",String.class,i.itemId()));
            return new Requirement(i.itemId(),conversion(i.quantity().multiply(factor),i.unit(),itemUnit)); }).toList();
    }
    private BigDecimal conversion(BigDecimal value, UnitOfMeasure from, UnitOfMeasure to) {
        if(from==to)return value; if(from==UnitOfMeasure.KG&&to==UnitOfMeasure.G||from==UnitOfMeasure.L&&to==UnitOfMeasure.ML)return value.multiply(BigDecimal.valueOf(1000));
        if(from==UnitOfMeasure.G&&to==UnitOfMeasure.KG||from==UnitOfMeasure.ML&&to==UnitOfMeasure.L)return value.divide(BigDecimal.valueOf(1000),9,RoundingMode.HALF_UP);
        throw new DomainException("INCOMPATIBLE_UNITS");
    }
    private RecipeView recipeRow(UUID id) {
        List<RecipeView> values=jdbc.query("SELECT r.*,i.name output_name FROM recipes r JOIN items i ON i.id=r.output_item_id WHERE r.id=?",(rs,n)->new RecipeView(
            rs.getObject("id",UUID.class),rs.getString("name"),rs.getObject("output_item_id",UUID.class),rs.getString("output_name"),rs.getBigDecimal("yield_quantity"),UnitOfMeasure.valueOf(rs.getString("yield_unit")),
            rs.getObject("shelf_life_days",Integer.class),rs.getBoolean("active"),rs.getString("notes"),ingredients(id),instant(rs,"created_at"),instant(rs,"updated_at")),id);
        if(values.isEmpty())throw new DomainException("RECIPE_NOT_FOUND"); return values.getFirst();
    }
    private List<IngredientView> ingredients(UUID recipeId){return jdbc.query("SELECT ri.*,i.name item_name FROM recipe_ingredients ri JOIN items i ON i.id=ri.item_id WHERE recipe_id=? ORDER BY i.name",
        (rs,n)->new IngredientView(rs.getObject("id",UUID.class),rs.getObject("item_id",UUID.class),rs.getString("item_name"),rs.getBigDecimal("quantity"),UnitOfMeasure.valueOf(rs.getString("unit"))),recipeId);}
    private ProductionOrderView order(UUID id, boolean details) {
        List<OrderBase> rows=jdbc.query("SELECT po.*,r.name recipe_name,r.output_item_id,i.name output_name,r.yield_unit FROM production_orders po JOIN recipes r ON r.id=po.recipe_id JOIN items i ON i.id=r.output_item_id WHERE po.id=?",this::orderBase,id);
        if(rows.isEmpty())throw new DomainException("PRODUCTION_ORDER_NOT_FOUND"); OrderBase b=rows.getFirst();
        List<RequirementView> req=details?requirementsView(b):List.of(); List<InventoryStockOperations.Allocation> consumed=details?consumptions(id):List.of();
        return new ProductionOrderView(b.id(),b.code(),b.recipeId(),b.recipeName(),b.outputItemId(),b.outputItemName(),b.plannedQuantity(),b.actualQuantity(),b.unit(),b.status(),b.plannedDate(),b.startedAt(),b.completedAt(),b.differenceReason(),b.notes(),req,consumed,details?output(id):null,b.createdAt(),b.updatedAt());
    }
    private List<RequirementView> requirementsView(OrderBase b){List<Requirement> req=requirements(b.recipeId(),b.plannedQuantity());Map<UUID,UnitOfMeasure> units=new HashMap<>();recipeRow(b.recipeId()).ingredients().forEach(i->units.put(i.itemId(),UnitOfMeasure.valueOf(jdbc.queryForObject("SELECT unit FROM items WHERE id=?",String.class,i.itemId()))));return stock.preview(req).stream().map(a->RequirementView.from(a,units.get(a.itemId()))).toList();}
    private List<InventoryStockOperations.Allocation> consumptions(UUID id){return jdbc.query("SELECT pc.item_id,i.name,pc.batch_id,b.code,pc.location_id,l.name,b.expiration_date,pc.quantity FROM production_consumptions pc JOIN items i ON i.id=pc.item_id JOIN batches b ON b.id=pc.batch_id JOIN locations l ON l.id=pc.location_id WHERE production_order_id=? ORDER BY pc.created_at",
        (rs,n)->new InventoryStockOperations.Allocation(rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,UUID.class),rs.getString(4),rs.getObject(5,UUID.class),rs.getString(6),rs.getObject(7,LocalDate.class),rs.getBigDecimal(8)),id);}
    private OutputView output(UUID id){List<OutputView> values=jdbc.query("SELECT po.item_id,i.name,po.batch_id,b.code,po.location_id,l.name,po.quantity FROM production_outputs po JOIN items i ON i.id=po.item_id JOIN batches b ON b.id=po.batch_id JOIN locations l ON l.id=po.location_id WHERE production_order_id=?",
        (rs,n)->new OutputView(rs.getObject(1,UUID.class),rs.getString(2),rs.getObject(3,UUID.class),rs.getString(4),rs.getObject(5,UUID.class),rs.getString(6),rs.getBigDecimal(7)),id);return values.isEmpty()?null:values.getFirst();}
    private OrderBase lockOrder(UUID id){List<OrderBase> rows=jdbc.query("SELECT po.*,r.name recipe_name,r.output_item_id,i.name output_name,r.yield_unit FROM production_orders po JOIN recipes r ON r.id=po.recipe_id JOIN items i ON i.id=r.output_item_id WHERE po.id=? FOR UPDATE OF po",this::orderBase,id);if(rows.isEmpty())throw new DomainException("PRODUCTION_ORDER_NOT_FOUND");return rows.getFirst();}
    private OrderBase orderBase(ResultSet rs,int n)throws SQLException{return new OrderBase(rs.getObject("id",UUID.class),rs.getString("code"),rs.getObject("recipe_id",UUID.class),rs.getString("recipe_name"),rs.getObject("output_item_id",UUID.class),rs.getString("output_name"),rs.getBigDecimal("planned_quantity"),rs.getBigDecimal("actual_quantity"),UnitOfMeasure.valueOf(rs.getString("yield_unit")),ProductionStatus.valueOf(rs.getString("status")),rs.getObject("planned_date",LocalDate.class),instant(rs,"started_at"),instant(rs,"completed_at"),rs.getString("difference_reason"),rs.getString("notes"),instant(rs,"created_at"),instant(rs,"updated_at"));}
    private Instant instant(ResultSet rs,String column)throws SQLException{var value=rs.getTimestamp(column);return value==null?null:value.toInstant();}
    private record OrderBase(UUID id,String code,UUID recipeId,String recipeName,UUID outputItemId,String outputItemName,BigDecimal plannedQuantity,BigDecimal actualQuantity,UnitOfMeasure unit,ProductionStatus status,LocalDate plannedDate,Instant startedAt,Instant completedAt,String differenceReason,String notes,Instant createdAt,Instant updatedAt){}
    private void validateActiveLocation(UUID id){Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM locations WHERE id=? AND active=true",Integer.class,id);if(count==null||count==0)throw new DomainException("INVALID_LOCATION");}
    private void requireExists(String table,UUID id,String error){Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE id=?",Integer.class,id);if(count==null||count==0)throw new DomainException(error);}
    private BigDecimal positive(BigDecimal value){return value!=null&&value.signum()>0?value:null;} private BigDecimal nonNegative(BigDecimal value){if(value==null||value.signum()<0)throw new DomainException("INVALID_QUANTITY");return value;}
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
}
