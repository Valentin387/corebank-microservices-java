package com.corebank.auth.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void testStringRedisTemplate() {
        // Arrange
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);

        // Act
        StringRedisTemplate template = config.stringRedisTemplate(mockFactory);

        // Assert
        assertNotNull(template);
        assertSame(mockFactory, template.getConnectionFactory());
    }
}
