package com.tayota.commoncore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
// Chỉ chạy class này nếu trong project có thư viện RedisTemplate (tức là service có cài thư viện spring-boot-starter-data-redis)
@ConditionalOnClass(RedisTemplate.class)
// Chỉ chạy class này nếu trong file properties/yml CÓ cấu hình spring.data.redis.host
@ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}") // Thêm :6379 để có giá trị mặc định nếu lỡ quên config port
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        return template;
    }
}
