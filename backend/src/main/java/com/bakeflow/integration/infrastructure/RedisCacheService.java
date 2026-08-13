package com.bakeflow.integration.infrastructure;

import com.bakeflow.integration.application.CacheService;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheService implements CacheService {
    private static final Logger log=LoggerFactory.getLogger(RedisCacheService.class);
    private final StringRedisTemplate redis;
    public RedisCacheService(StringRedisTemplate redis){this.redis=redis;}
    public Optional<String> get(String key){try{return Optional.ofNullable(redis.opsForValue().get(key));}catch(RuntimeException e){log.warn("Redis operation unavailable operation=get");return Optional.empty();}}
    public void set(String key,String value,Duration ttl){try{redis.opsForValue().set(key,value,ttl);}catch(RuntimeException e){log.warn("Redis operation unavailable operation=set");}}
    public void delete(String key){try{redis.delete(key);}catch(RuntimeException e){log.warn("Redis operation unavailable operation=delete");}}
    public boolean exists(String key){try{return Boolean.TRUE.equals(redis.hasKey(key));}catch(RuntimeException e){return false;}}
    public boolean incrementWithinLimit(String key,int limit,Duration window){try{Long count=redis.opsForValue().increment(key);if(count!=null&&count==1)redis.expire(key,window);return count!=null&&count<=limit;}catch(RuntimeException e){log.warn("Redis rate limiter unavailable; external call fails closed");throw new com.bakeflow.integration.application.IntegrationException("EXTERNAL_SERVICE_UNAVAILABLE");}}
    public boolean available(){try{return "PONG".equals(redis.getConnectionFactory().getConnection().ping());}catch(RuntimeException e){return false;}}
}
