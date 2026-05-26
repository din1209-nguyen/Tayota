package com.tayota.operationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    // Khai báo cache manager riêng cho nhóm dữ liệu xe để cấu hình TTL theo từng nhóm cache
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // Tạo serializer JSON dùng chung với RedisTemplate trong common code
        RedisSerializer<Object> jsonRedisSerializer = new LegacyAwareJsonRedisSerializer(objectMapper, false);

        // Tạo cấu hình cache mặc định
        RedisCacheConfiguration defaultConfig = createCacheConfiguration(jsonRedisSerializer, Duration.ofMinutes(30));

        // Tạo cấu hình TTL riêng cho từng cache
        Map<String, RedisCacheConfiguration> cacheConfigs = new LinkedHashMap<>();
        cacheConfigs.put("catalogStylesWithVersions", createCacheConfiguration(jsonRedisSerializer, Duration.ofHours(2)));
        cacheConfigs.put("catalogVersionDetail", createCacheConfiguration(jsonRedisSerializer, Duration.ofHours(1)));
        cacheConfigs.put("catalogSpecification", createCacheConfiguration(jsonRedisSerializer, Duration.ofHours(6)));
        cacheConfigs.put("catalogVersionSearch", createCacheConfiguration(jsonRedisSerializer, Duration.ofMinutes(15)));
        cacheConfigs.put("accessorySearch", createCacheConfiguration(jsonRedisSerializer, Duration.ofMinutes(15)));
        cacheConfigs.put("accessoryDetail", createCacheConfiguration(jsonRedisSerializer, Duration.ofMinutes(30)));
        cacheConfigs.put("physicalCarSearch", createCacheConfiguration(jsonRedisSerializer, Duration.ofMinutes(2)));
        cacheConfigs.put("carStyleList", createCacheConfiguration(jsonRedisSerializer, Duration.ofHours(2)));
        cacheConfigs.put("carStyleDetail", createCacheConfiguration(jsonRedisSerializer, Duration.ofHours(2)));
        cacheConfigs.put("carSeriesList", createCacheConfiguration(jsonRedisSerializer, Duration.ofHours(2)));
        cacheConfigs.put("carSeriesDetail", createCacheConfiguration(jsonRedisSerializer, Duration.ofHours(2)));
        cacheConfigs.put("carVersionList", createCacheConfiguration(jsonRedisSerializer, Duration.ofMinutes(30)));

        // Trả về RedisCacheManager với cấu hình TTL đã khai báo
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
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
                // Bỏ qua lỗi ghi cache để Redis không làm hỏng luồng nghiệp vụ chính.
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                // Bỏ qua lỗi xóa cache đơn lẻ khi Redis tạm thời không sẵn sàng.
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                // Bỏ qua lỗi xóa toàn bộ cache khi Redis tạm thời không sẵn sàng.
            }
        };
    }

    // Tạo cấu hình cache theo TTL truyền vào
    private RedisCacheConfiguration createCacheConfiguration(
            RedisSerializer<Object> jsonRedisSerializer,
            Duration ttl
    ) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer))
                .disableCachingNullValues();
    }
}
