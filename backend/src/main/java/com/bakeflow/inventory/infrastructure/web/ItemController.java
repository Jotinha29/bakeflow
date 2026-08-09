package com.bakeflow.inventory.infrastructure.web;
import com.bakeflow.inventory.application.*;import com.bakeflow.inventory.application.InventoryDtos.*;import com.bakeflow.inventory.domain.ItemType;
import java.net.URI;import java.util.UUID;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/items") public class ItemController{
 private final InventoryCatalogService service;ItemController(InventoryCatalogService service){this.service=service;}
 @GetMapping public PageResult<ItemView>list(@RequestParam(required=false)String search,@RequestParam(required=false)String sku,@RequestParam(required=false)ItemType type,@RequestParam(required=false)Boolean active,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.searchItems(search,sku,type,active,page,size);}
 @GetMapping("/{id}")public ItemView get(@PathVariable UUID id){return service.getItem(id);}
 @PostMapping public ResponseEntity<ItemView>create(@RequestBody ItemInput input){var item=service.createItem(input);return ResponseEntity.created(URI.create("/api/v1/items/"+item.id())).body(item);}
 @PutMapping("/{id}")public ItemView update(@PathVariable UUID id,@RequestBody ItemInput input){return service.updateItem(id,input);}
 @PatchMapping("/{id}/activate")public ItemView activate(@PathVariable UUID id){return service.setItemActive(id,true);}
 @PatchMapping("/{id}/deactivate")public ItemView deactivate(@PathVariable UUID id){return service.setItemActive(id,false);}
}
