package com.tayota.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

// Cấu hình CORS (Cross-Origin Resource Sharing)
// Cho phép gọi từ các domain hợp lệ, và cho phép gửi cookie
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        // Tạo đối tượng dùng để lưu và quản lý các cấu hình CORS
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Áp dụng bộ config CORS cho tất cả đường dẫn
        source.registerCorsConfiguration("/**", buildCorsConfiguration());

        // Spring tự động đưa Filter này vào đầu Filter Chain, đảm bảo CORS được xử lý trước AuthenticationFilter
        return new CorsWebFilter(source);
    }

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();

        // Chỉ cho phép request từ Frontend
        // KHÔNG dùng "*" khi allowCredentials = true → browser sẽ báo lỗi
        config.addAllowedOrigin("http://localhost:3000");

        // Cho phép tất cả HTTP method (GET, POST, PUT, DELETE, PATCH, OPTIONS...)
        config.addAllowedMethod("*");

        // Cho phép tất cả header, bao gồm Authorization, Content-Type, X-User-Id...
        config.addAllowedHeader("*");

        // Bắt buộc true nếu Frontend gửi Authorization: Bearer token
        config.setAllowCredentials(true);

        // Thời gian cache kết quả Preflight (OPTIONS) — đơn vị: giây
        // Nên set = 0 khi dev để tránh cache gây khó debug
        // config.setMaxAge(3600L);
        config.setMaxAge(0L);

        return config;
    }
}