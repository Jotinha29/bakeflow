package com.bakeflow.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponse {
    private final ObjectMapper json;

    public SecurityErrorResponse(ObjectMapper json) { this.json = json; }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String code)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("code", code);
        body.put("message", code);
        Object requestId = request.getAttribute("X-Request-ID");
        if (requestId != null) body.put("requestId", requestId);
        json.writeValue(response.getOutputStream(), body);
    }
}
