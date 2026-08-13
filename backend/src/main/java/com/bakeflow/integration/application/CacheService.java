package com.bakeflow.integration.application;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {
    Optional<String> get(String key);
    void set(String key, String value, Duration ttl);
    void delete(String key);
    boolean exists(String key);
    boolean incrementWithinLimit(String key, int limit, Duration window);
    boolean available();
}
