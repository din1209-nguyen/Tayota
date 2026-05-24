package com.tayota.operationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    // Khai báo cache manager riêng cho nhóm dữ liệu xe để cấu hình TTL theo từng nhóm cache
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // Tạo serializer JSON dùng chung với RedisTemplate trong common code
        GenericJacksonJsonRedisSerializer jsonRedisSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);

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

    // Tạo cấu hình cache theo TTL truyền vào
    private RedisCacheConfiguration createCacheConfiguration(
            GenericJacksonJsonRedisSerializer jsonRedisSerializer,
            Duration ttl
    ) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer))
                .disableCachingNullValues();
    }
}
