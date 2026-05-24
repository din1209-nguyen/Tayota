package com.tayota.operationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // Khởi tạo RedisTemplate<String, Object>
        // - Key: String (key cache sẽ là chuỗi text bình thường)
        // - Value: Object (có thể lưu bất kỳ object Java nào)
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // Gán connection factory để template có thể kết nối tới Redis server
        template.setConnectionFactory(connectionFactory);

        // Tạo ObjectMapper tùy chỉnh cho serialization
        GenericJacksonJsonRedisSerializer jsonRedisSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);

        // Cấu hình Serializer cho Key
        // Serializer: quy định cách chuyển đổi dữ liệu Java thành bytes khi lưu vào Redis
        // Ép Key luôn lưu dưới dạng String thuần tuý (không mã hóa)
        // Đảm bảo key dễ đọc khi dùng Redis CLI hoặc tools quản lý Redis
        template.setKeySerializer(new StringRedisSerializer());

        // Cấu hình Serializer cho Value
        // Dùng GenericJacksonJsonRedisSerializer để tự động convert Object -> JSON
        // Khi lưu: Object Java -> JSON string
        // Khi lấy ra: JSON string -> Object Java
        template.setValueSerializer(jsonRedisSerializer);

        // Cấu hình cho Hash
        // Dùng cho các thao tác với cấu trúc Hash trong Redis
        // Ví dụ: hset, hget, hgetall
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonRedisSerializer);

        // Trả về RedisTemplate đã cấu hình xong cho Spring quản lý
        return template;
    }

    /* --- Cấu hình tự động (Cache Management) --- */
    // Dùng kết hợp với @Cacheable, @CachePut, @CacheEvict annotations
    // Giúp tự động cache kết quả hàm mà không cần code thủ công
    @Bean
    @ConditionalOnMissingBean(name = "cacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // Tạo ObjectMapper tùy chỉnh cho Cache Manager
        GenericJacksonJsonRedisSerializer jsonRedisSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);

        // Thiết lập cấu hình mặc định cho toàn bộ cache
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                // TTL (Time To Live): dữ liệu sẽ tự động xóa sau 1 giờ
                // Tránh Redis bị lấp đầy với dữ liệu cũ không còn cần thiết
                .entryTtl(Duration.ofHours(1))

                // Cấu hình serializer cho Cache Key
                // Dùng StringRedisSerializer để key dễ nhìn trong Redis CLI
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))

                // Cấu hình serializer cho Cache Value
                // Dùng JSON để tất cả các object type đều có thể serialize/deserialize
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonRedisSerializer))

                // Không cache các kết quả null
                // Giúp tiết kiệm bộ nhớ Redis và tránh lỗi nhập nhằng khi null
                // (vì không biết là null do method không return hay là null được cache)
                .disableCachingNullValues();

        // Xây dựng RedisCacheManager với cấu hình trên
        // nếu service riêng định nghĩa @Bean("cacheManager") thì sẽ dùng cái riêng
        // nếu không, sẽ dùng cấu hình mặc định này từ common code
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}