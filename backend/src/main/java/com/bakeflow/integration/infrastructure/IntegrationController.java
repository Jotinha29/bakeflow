package com.bakeflow.integration.infrastructure;
import com.bakeflow.integration.application.*;import com.bakeflow.integration.application.IntegrationDtos.*;import jakarta.servlet.http.HttpServletRequest;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/integrations") public class IntegrationController{
 private final IntegrationService service;public IntegrationController(IntegrationService service){this.service=service;}
 @GetMapping("/product/{barcode}") ExternalProductResult product(@PathVariable String barcode,HttpServletRequest request){return service.product(barcode,client(request));}
 @GetMapping("/company/{cnpj}") ExternalCompanyResult company(@PathVariable String cnpj,HttpServletRequest request){return service.company(cnpj,client(request));}
 private String client(HttpServletRequest r){return r.getRemoteAddr();}
}
