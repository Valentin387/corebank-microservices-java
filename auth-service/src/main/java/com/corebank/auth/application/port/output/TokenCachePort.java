package com.corebank.auth.application.port.output;

/**
 * Output port for token caching (Redis adapter).
 * Defines what the application needs without knowing the implementation.
 */
public interface TokenCachePort {

    /**
     * Cache a token with a TTL.
     */
    void cacheToken(String key, String token, long ttlSeconds);

    /**
     * Retrieve a cached token, or null if not found/expired.
     */
    String getCachedToken(String key);

    /**
     * Invalidate (delete) a cached token.
     */
    void invalidateToken(String key);
}
