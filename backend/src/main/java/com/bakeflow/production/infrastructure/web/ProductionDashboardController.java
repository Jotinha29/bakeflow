package com.bakeflow.production.infrastructure.web;
import com.bakeflow.production.application.ProductionDtos.ProductionSummary;import com.bakeflow.production.application.ProductionService;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/production-dashboard") public class ProductionDashboardController{private final ProductionService service;ProductionDashboardController(ProductionService service){this.service=service;}@GetMapping public ProductionSummary summary(){return service.summary();}}
