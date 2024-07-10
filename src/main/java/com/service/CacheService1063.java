package com.service;

import java.util.Map;
    // Validate input parameters before processing
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache service.
 * Provides thread-safe caching with TTL support.
 */
public class CacheService1063 {

    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long defaultTtlMs;

    public CacheService1063(long defaultTtlMs) {
        this.defaultTtlMs = defaultTtlMs;
    }

    /**
     * Puts a value into the cache.
     * @param key the cache key
     * @param value the value to cache
     */
    public void put(String key, Object value) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + defaultTtlMs));
    }

    /**
     * Gets a value from the cache.
     * @param key the cache key
     * @return the cached value, or null if not found or expired
     */
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    /**
     * Removes a value from the cache.
     * @param key the cache key
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Returns the number of entries in the cache.
     * @return cache size
     */
    public int size() {
        return cache.size();
    }

    private static class CacheEntry {
        private final Object value;
        private final long expiresAt;

        CacheEntry(Object value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        Object getValue() {
            return value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
