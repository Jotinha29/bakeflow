package com.bakeflow.production.infrastructure.web;
import static com.bakeflow.production.application.ProductionDtos.*;
import com.bakeflow.production.application.ProductionService;import com.bakeflow.production.domain.ProductionStatus;import java.net.URI;import java.time.LocalDate;import java.util.*;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/production-orders") public class ProductionOrderController{
 private final ProductionService service;ProductionOrderController(ProductionService service){this.service=service;}
 @GetMapping public List<ProductionOrderView>list(@RequestParam(required=false)String code,@RequestParam(required=false)UUID recipeId,@RequestParam(required=false)ProductionStatus status,@RequestParam(required=false)LocalDate plannedDate){return service.orders(code,recipeId,status,plannedDate);}
 @GetMapping("/{id}")public ProductionOrderView get(@PathVariable UUID id){return service.getOrder(id);}
 @GetMapping("/{id}/preview")public ProductionOrderView preview(@PathVariable UUID id){return service.preview(id);}
 @PostMapping public ResponseEntity<ProductionOrderView>create(@RequestBody OrderInput input){var value=service.createOrder(input);return ResponseEntity.created(URI.create("/api/v1/production-orders/"+value.id())).body(value);}
 @PostMapping("/preview")public ProductionPreview previewPlan(@RequestBody PreviewInput input){return service.previewPlan(input);}
 @PostMapping("/{id}/start")public ProductionOrderView start(@PathVariable UUID id){return service.start(id);}
 @PostMapping("/{id}/complete")public ProductionOrderView complete(@PathVariable UUID id,@RequestBody CompleteInput input){return service.complete(id,input);}
 @PostMapping("/{id}/cancel")public ProductionOrderView cancel(@PathVariable UUID id){return service.cancel(id);}
}
