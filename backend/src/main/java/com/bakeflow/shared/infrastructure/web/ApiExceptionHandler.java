package com.bakeflow.shared.infrastructure.web;

import com.bakeflow.identity.IdentityException;
import com.bakeflow.integration.application.IntegrationException;
import com.bakeflow.inventory.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<Map<String, Object>> domain(DomainException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException exception,
            HttpServletRequest request) {
        log.warn("Database integrity operation failed");
        return error(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION", "Operation conflicts with existing data.",
                List.of(), request);
    }

    @ExceptionHandler(IntegrationException.class)
    ResponseEntity<Map<String, Object>> integration(IntegrationException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.code()) {
            case "INVALID_CNPJ", "INVALID_BARCODE" -> HttpStatus.BAD_REQUEST;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return error(status, exception.code(), exception.code(), List.of(), request);
    }

    @ExceptionHandler(IdentityException.class)
    ResponseEntity<Map<String, Object>> identity(IdentityException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.code()) {
            case "INVALID_CREDENTIALS", "INVALID_REFRESH_TOKEN", "REFRESH_TOKEN_REUSED", "UNAUTHENTICATED" ->
                    HttpStatus.UNAUTHORIZED;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "SESSION_NOT_FOUND", "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_ORIGIN" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return error(status, exception.code(), exception.code(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<Map<String, String>> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(this::field).toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request data.", fields, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> mismatch(MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_QUERY_PARAMETER", "Invalid query parameter.",
                List.of(Map.of("field", exception.getName(), "message", "Invalid value.")), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> malformed(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed JSON request.", List.of(), request);
    }

    private Map<String, String> field(FieldError error) {
        return Map.of("field", error.getField(), "message",
                error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message,
            List<Map<String, String>> errors, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        Object requestId = request.getAttribute("X-Request-ID");
        if (requestId != null) body.put("requestId", requestId);
        if (!errors.isEmpty()) body.put("errors", errors);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (status == HttpStatus.TOO_MANY_REQUESTS) response.header("Retry-After", "60");
        return response.body(body);
    }
}
