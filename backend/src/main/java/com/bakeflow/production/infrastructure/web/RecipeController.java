package com.bakeflow.production.infrastructure.web;
import static com.bakeflow.production.application.ProductionDtos.*;
import com.bakeflow.production.application.ProductionService;import java.net.URI;import java.util.*;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/recipes") public class RecipeController{
 private final ProductionService service;RecipeController(ProductionService service){this.service=service;}
 @GetMapping public List<RecipeView>list(@RequestParam(required=false)String search,@RequestParam(required=false)UUID outputItemId,@RequestParam(required=false)Boolean active){return service.recipes(search,outputItemId,active);}
 @GetMapping("/{id}")public RecipeView get(@PathVariable UUID id){return service.recipe(id);}
 @PostMapping public ResponseEntity<RecipeView>create(@RequestBody RecipeInput input){var value=service.createRecipe(input);return ResponseEntity.created(URI.create("/api/v1/recipes/"+value.id())).body(value);}
 @PutMapping("/{id}")public RecipeView update(@PathVariable UUID id,@RequestBody RecipeInput input){return service.updateRecipe(id,input);}
 @PatchMapping("/{id}/activate")public void activate(@PathVariable UUID id){service.setRecipeActive(id,true);}
 @PatchMapping("/{id}/deactivate")public void deactivate(@PathVariable UUID id){service.setRecipeActive(id,false);}
}
