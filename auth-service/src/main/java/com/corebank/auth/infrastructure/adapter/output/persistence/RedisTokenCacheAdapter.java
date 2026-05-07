package com.corebank.auth.infrastructure.adapter.output.persistence;

import com.corebank.auth.application.port.output.TokenCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis adapter implementing the TokenCachePort output port.
 * Handles token caching/retrieval/invalidation.
 */
@Component
public class RedisTokenCacheAdapter implements TokenCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenCacheAdapter.class);

    private final StringRedisTemplate redisTemplate;

    public RedisTokenCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cacheToken(String key, String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, token, ttlSeconds, TimeUnit.SECONDS);
        log.debug("Cached token for key: {}", key);
    }

    @Override
    public String getCachedToken(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void invalidateToken(String key) {
        redisTemplate.delete(key);
        log.debug("Invalidated token for key: {}", key);
    }
}
