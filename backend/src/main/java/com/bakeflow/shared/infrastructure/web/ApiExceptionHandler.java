package com.bakeflow.shared.infrastructure.web;
import com.bakeflow.inventory.domain.DomainException;import com.bakeflow.integration.application.IntegrationException;import java.time.Instant;import java.util.Map;import org.slf4j.*;import org.springframework.dao.DataIntegrityViolationException;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler{
 private static final Logger log=LoggerFactory.getLogger(ApiExceptionHandler.class);
 @ExceptionHandler(DomainException.class)ResponseEntity<Map<String,Object>>domain(DomainException e){return error(HttpStatus.BAD_REQUEST,e.getMessage());}
 @ExceptionHandler(DataIntegrityViolationException.class)ResponseEntity<Map<String,Object>>integrity(DataIntegrityViolationException e){log.warn("Database integrity operation failed",e);return error(HttpStatus.CONFLICT,"The requested value already exists or violates a catalog constraint.");}
 @ExceptionHandler(IntegrationException.class)ResponseEntity<Map<String,Object>>integration(IntegrationException e){HttpStatus status=switch(e.code()){case "INVALID_CNPJ","INVALID_BARCODE"->HttpStatus.BAD_REQUEST;case "RATE_LIMIT_EXCEEDED"->HttpStatus.TOO_MANY_REQUESTS;default->HttpStatus.SERVICE_UNAVAILABLE;};var response=error(status,e.code());if(status==HttpStatus.TOO_MANY_REQUESTS)response.getHeaders().set("Retry-After","60");return response;}
 private ResponseEntity<Map<String,Object>>error(HttpStatus status,String message){return ResponseEntity.status(status).body(Map.of("timestamp",Instant.now().toString(),"status",status.value(),"message",message));}
}
