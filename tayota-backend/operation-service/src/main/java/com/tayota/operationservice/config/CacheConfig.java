package com.tayota.operationservice.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.support.SimpleCacheManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CacheConfig implements CachingConfigurer {

    // Khai báo cache manager in-memory cho nhóm dữ liệu xe để cấu hình TTL theo từng nhóm cache.
    @Bean
    public CacheManager cacheManager() {
        List<CaffeineCache> caches = new ArrayList<>();
        caches.add(createCache("catalogStylesWithVersions", Duration.ofHours(2)));
        caches.add(createCache("catalogVersionDetail", Duration.ofHours(1)));
        caches.add(createCache("catalogSpecification", Duration.ofHours(6)));
        caches.add(createCache("catalogVersionSearch", Duration.ofMinutes(15)));
        caches.add(createCache("accessorySearch", Duration.ofMinutes(15)));
        caches.add(createCache("accessoryDetail", Duration.ofMinutes(30)));
        caches.add(createCache("physicalCarSearch", Duration.ofMinutes(2)));
        caches.add(createCache("carStyleList", Duration.ofHours(2)));
        caches.add(createCache("carStyleDetail", Duration.ofHours(2)));
        caches.add(createCache("carSeriesList", Duration.ofHours(2)));
        caches.add(createCache("carSeriesDetail", Duration.ofHours(2)));
        caches.add(createCache("carVersionList", Duration.ofMinutes(30)));

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                try {
                    cache.evictIfPresent(key);
                }
                catch (RuntimeException ignored) {
                    // Bỏ qua lỗi xóa cache để request tiếp tục đọc dữ liệu từ database.
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                // Bỏ qua lỗi ghi cache để cache hệ thống không làm hỏng luồng nghiệp vụ chính.
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                // Bỏ qua lỗi xóa cache đơn lẻ khi cache hệ thống tạm thời không sẵn sàng.
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                // Bỏ qua lỗi xóa toàn bộ cache khi cache hệ thống tạm thời không sẵn sàng.
            }
        };
    }

    // Tạo cache in-memory theo TTL truyền vào.
    private CaffeineCache createCache(String name, Duration ttl) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(10_000)
                .build(), false);
    }
}
