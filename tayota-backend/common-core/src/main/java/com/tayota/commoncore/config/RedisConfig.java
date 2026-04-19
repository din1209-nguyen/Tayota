package com.tayota.commoncore.config;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {
    // Đối tượng của Jackson dùng để chuyển đổi (parse/serialize) qua lại giữa Java Object và chuỗi JSON
    private final ObjectMapper objectMapper;

    // Cấu hình thủ công (Dùng RedisTemplate)
    // Cung cấp một công cụ để dev có thể tự code các thao tác CRUD với Redis
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // Khởi tạo một RedisTemplate nhận Key là String và Value là một Object bất kỳ
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        /*
        * Mặc định Redis chỉ lưu dữ liệu dạng bytes (chuỗi nhị phân) nên sẽ khó nhìn vào
        * Nên sẽ chuẩn đổi dữ liệu thành JSON dễ đọc hơn khi lưu vào Redis
        */

        // Cấp cho template một kết nối tới server Redis (mặc định Spring dùng thư viện Lettuce hoặc Jedis)
        template.setConnectionFactory(connectionFactory);

        // Ép Key luôn được lưu dưới dạng String thuần tuý
        template.setKeySerializer(new StringRedisSerializer());
        // Dùng GenericJacksonJsonRedisSerializer kết hợp với objectMapper
        // Khi lưu một Object Java vào Redis, nó sẽ tự động biến thành một chuỗi JSON
        // Áp dụng cho cấu trúc String, List, Set ZSet,...
        template.setValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));

        // Cấu hình tương tự cho cấu trúc dữ liệu Hash (như Map<String, Object> trong Java)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));

        // Hoàn tất và trả về bean cho Spring quản lý
        return template;
    }

     // Cấu hình tự động (Cache Management)
     // Dùng cho các Annotation như @Cacheable, @CachePut, @CacheEvict
    @Bean
    // Nếu trong service chưa khai báo Bean tên là 'cacheManager', Spring sẽ chạy vào common-core này và dùng cấu hình mặc định
    @ConditionalOnMissingBean(name = "cacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // Thiết lập các luật (Configuration) cho Cache
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Thời gian sống mặc định của data trong Redis 1 giờ
                .entryTtl(Duration.ofHours(1))
                // Cấu hình cách mã hoá Key cho Cache tự động: Dùng StringSerializer để Key dễ đọc
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Cấu hình cách mã hoá Value cho Cache tự động: Dùng JSON Serializer giống hệt ở trên
                // Truyền objectMapper vào để đảm bảo luật parse JSON (ví dụ format ngày tháng) đồng nhất toàn dự án
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(objectMapper)))
                // Nếu hàm trả về kết quả là null, tuyệt đối không lưu chữ "null" đó vào Redis
                // Tránh tốn dung lượng vô ích và ngăn chặn lỗi mập mờ khi lấy data ra
                .disableCachingNullValues();

        // Thiết lập trên vào một CacheManager thông qua Builder pattern và trả về
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}