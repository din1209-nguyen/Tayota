package com.tayota.commoncore.config;

import com.tayota.commoncore.filter.HeaderAuthenticationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Tự động cấu hình, ưu tiên load sau các cấu hình của người dùng để không ghi đè cấu hình chính
@AutoConfiguration
// Bật hệ thống Spring Security cho Web (HTTP request)
@EnableWebSecurity
// Bật Security ở phạm vi method (hàm), cho phép dùng @PreAuthorize,...
@EnableMethodSecurity
// Dùng để bật/tắt một bean hoặc configuration dựa trên việc ứng dụng có phải là Web Application hay không
@ConditionalOnWebApplication
// - Tạo "lối thoát" cho dev. Nếu có service nào đó (như Auth Service) muốn tự code Security riêng,
// thì chỉ cần thêm `common.security.enabled=false` vào file application.yml để tắt thư viện này
// - matchIfMissing = true: mặc định Bật nếu không khai báo common.security.enabled=false
@ConditionalOnProperty(prefix = "common.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    // Khai báo Filter dưới dạng Bean thay vì dùng @Component ở class kia.
    // Cách này giúp thư viện quản lý vòng đời (lifecycle) của đối tượng sạch sẽ hơn.
    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   HeaderAuthenticationFilter headerAuthenticationFilter) throws Exception {
        http
                // Cross-Site Request Forgery: mặc định mọi request POST, PUT, DELETE phải có CSRF token, vì dùng JWT nên không cần
                .csrf(AbstractHttpConfigurer::disable)
                // Không dùng session để lưu trạng thái vì ta dùng JWT tự quản lý trạng thái nêu mỗi request sẽ độc lập với nhau
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Phân quyền các đường dẫn, vì đã lọc từ API-Gateway nên trong các Services sẽ cho phép tất cả
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Spring sẽ chạy HeaderAuthenticationFilter trước để lấy thông tin từ Header
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}