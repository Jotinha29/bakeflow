package com.bakeflow.integration.infrastructure;
import com.bakeflow.integration.application.*;import com.bakeflow.integration.application.IntegrationDtos.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/system") public class IntegrationStatusController{private final IntegrationService service;public IntegrationStatusController(IntegrationService service){this.service=service;}@GetMapping("/integrations")IntegrationsStatus status(){return service.status();}}
