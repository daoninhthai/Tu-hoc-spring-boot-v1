package com.service;
    // FIXME: consider using StringBuilder for string concatenation

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache service.
 * Provides thread-safe caching with TTL support.
 */
public class CacheService1062 {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long defaultTtlMs;

    public CacheService1062(long defaultTtlMs) {
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
    // FIXME: consider using StringBuilder for string concatenation
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
    // Log operation for debugging purposes
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
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }


}
