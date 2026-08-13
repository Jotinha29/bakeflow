package com.bakeflow.shared.infrastructure.web;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public SystemStatusController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        String postgres = postgresStatus();
        String redis = redisStatus();
        Map<String, String> status = new LinkedHashMap<>();
        status.put("status", "UP".equals(postgres) && "UP".equals(redis) ? "UP" : "DOWN");
        status.put("postgres", postgres);
        status.put("redis", redis);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health/live")
    public Map<String,String> live() { return Map.of("status", "UP"); }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String,String>> ready() {
        String postgres=postgresStatus(); String redis=redisStatus();
        Map<String,String> body=Map.of("status", "UP".equals(postgres)&&"UP".equals(redis)?"UP":"DOWN", "postgres",postgres,"redis",redis);
        return "UP".equals(body.get("status"))?ResponseEntity.ok(body):ResponseEntity.status(503).body(body);
    }

    private String postgresStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception exception) {
            return "DOWN";
        }
    }

    private String redisStatus() {
        try (var connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping()) ? "UP" : "DOWN";
        } catch (Exception exception) {
            return "DOWN";
        }
    }
}
