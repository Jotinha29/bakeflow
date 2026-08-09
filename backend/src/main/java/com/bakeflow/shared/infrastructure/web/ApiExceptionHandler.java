package com.bakeflow.shared.infrastructure.web;
import com.bakeflow.inventory.domain.DomainException;import java.time.Instant;import java.util.Map;import org.springframework.dao.DataIntegrityViolationException;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler{
 @ExceptionHandler(DomainException.class)ResponseEntity<Map<String,Object>>domain(DomainException e){return error(HttpStatus.BAD_REQUEST,e.getMessage());}
 @ExceptionHandler(DataIntegrityViolationException.class)ResponseEntity<Map<String,Object>>integrity(){return error(HttpStatus.CONFLICT,"The requested value already exists or violates a catalog constraint.");}
 private ResponseEntity<Map<String,Object>>error(HttpStatus status,String message){return ResponseEntity.status(status).body(Map.of("timestamp",Instant.now().toString(),"status",status.value(),"message",message));}
}
