package com.corebank.auth.infrastructure.adapter.output.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenCacheAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisTokenCacheAdapter adapter;

    @Test
    @DisplayName("cacheToken should store token with TTL in Redis")
    void cacheTokenShouldStoreWithTTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.cacheToken("auth:token:user", "jwt-token", 3600);

        verify(valueOperations).set("auth:token:user", "jwt-token", 3600, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("getCachedToken should retrieve token from Redis")
    void getCachedTokenShouldRetrieveFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:token:user")).thenReturn("cached-token");

        String result = adapter.getCachedToken("auth:token:user");

        assertEquals("cached-token", result);
    }

    @Test
    @DisplayName("getCachedToken should return null when not found")
    void getCachedTokenShouldReturnNullWhenNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:token:missing")).thenReturn(null);

        String result = adapter.getCachedToken("auth:token:missing");

        assertNull(result);
    }

    @Test
    @DisplayName("invalidateToken should delete key from Redis")
    void invalidateTokenShouldDeleteKey() {
        adapter.invalidateToken("auth:token:user");

        verify(redisTemplate).delete("auth:token:user");
    }
}
