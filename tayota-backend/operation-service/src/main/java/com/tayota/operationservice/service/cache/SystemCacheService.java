package com.tayota.operationservice.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SystemCacheService {
    private static final long NO_EXPIRY = Long.MAX_VALUE;

    private final Cache<String, CacheEntry> cache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build();

    public Object get(String key) {
        CacheEntry entry = getLiveEntry(key);
        return entry == null ? null : entry.value();
    }

    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void put(String key, Object value, Duration ttl) {
        cache.put(key, new CacheEntry(value, expiresAtMillis(ttl)));
    }

    public void delete(String key) {
        cache.invalidate(key);
    }

    public boolean hasKey(String key) {
        return getLiveEntry(key) != null;
    }

    public Long getExpire(String key) {
        CacheEntry entry = getLiveEntry(key);
        if (entry == null) {
            return -2L;
        }
        if (entry.expiresAtMillis() == NO_EXPIRY) {
            return -1L;
        }

        long remainingMillis = entry.expiresAtMillis() - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            cache.invalidate(key);
            return -2L;
        }
        return Math.max(1L, (long) Math.ceil(remainingMillis / 1000.0));
    }

    public void expire(String key, Duration ttl) {
        CacheEntry entry = getLiveEntry(key);
        if (entry != null) {
            cache.put(key, new CacheEntry(entry.value(), expiresAtMillis(ttl)));
        }
    }

    public synchronized Long increment(String key) {
        CacheEntry entry = getLiveEntry(key);
        long next = 1L;
        long expiresAt = NO_EXPIRY;
        if (entry != null) {
            Object value = entry.value();
            next = value instanceof Number number ? number.longValue() + 1 : Long.parseLong(value.toString()) + 1;
            expiresAt = entry.expiresAtMillis();
        }
        cache.put(key, new CacheEntry(next, expiresAt));
        return next;
    }

    public Set<Object> setMembers(String key) {
        Set<Object> values = get(key, Set.class);
        return values == null ? Set.of() : new LinkedHashSet<>(values);
    }

    public synchronized void setAdd(String key, Object value) {
        Set<Object> values = mutableSet(key);
        values.add(value);
        cache.put(key, new CacheEntry(values, currentExpiry(key)));
    }

    public synchronized void setRemove(String key, Object value) {
        Set<Object> values = mutableSet(key);
        values.remove(value);
        cache.put(key, new CacheEntry(values, currentExpiry(key)));
    }

    private Set<Object> mutableSet(String key) {
        Set<Object> existing = get(key, Set.class);
        Set<Object> values = ConcurrentHashMap.newKeySet();
        if (existing == null) {
            return values;
        }
        values.addAll(existing);
        return values;
    }

    private long currentExpiry(String key) {
        CacheEntry entry = getLiveEntry(key);
        return entry == null ? NO_EXPIRY : entry.expiresAtMillis();
    }

    private CacheEntry getLiveEntry(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMillis() != NO_EXPIRY && entry.expiresAtMillis() <= System.currentTimeMillis()) {
            cache.invalidate(key);
            return null;
        }
        return entry;
    }

    private long expiresAtMillis(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return NO_EXPIRY;
        }
        return System.currentTimeMillis() + ttl.toMillis();
    }

    private record CacheEntry(Object value, long expiresAtMillis) {
    }
}
