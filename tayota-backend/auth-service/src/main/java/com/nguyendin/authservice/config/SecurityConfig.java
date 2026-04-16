package com.nguyendin.authservice.config;

import com.nguyendin.authservice.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean // SecurityFilterChain là nơi cấu hình toàn bộ bảo mật HTTP
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        httpSecurity
                // Cross-Site Request Forgery: mặc định mọi request POST, PUT, DELETE phải có CSRF token, vì dùng JWT nên không cần
                .csrf(AbstractHttpConfigurer::disable)
                // Không dùng session để lưu trạng thái vì ta dùng JWT tự quản lý trạng thái nêu mỗi request sẽ độc lập với nhau
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Cấu hình quyền truy cập cho các HTTP request
                .authorizeHttpRequests(authorize ->
                        authorize
                                // Các request không cần xác thực
                                .requestMatchers("/register", "/login", "/verify-email", "/oauth/google", "/forgot-password", "/resend-verify-email", "/reset-password").permitAll()
                                // Các request còn lại bắt buộc xác thực
                                .anyRequest().authenticated())
                // Đăng ký AuthenticationProvider để xử lý login username/password
                .authenticationProvider(authenticationProvider());
        // Build và trả về chuỗi filter hoàn chỉnh cho Spring
        return httpSecurity.build();
    }

    @Bean // AuthenticationProvider dùng để xác thực email và password
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider sẽ:
        // 1. Gọi CustomUserDetailsService để lấy user từ DB
        // 2. So sánh password đã mã hóa
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        // Thiết lập thuật toán mã hóa mật khẩu (BCrypt)
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    // AuthenticationManager chịu trách nhiệm điều phối quá trình xác thực khi nhận email và password
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {
        // Lấy AuthenticationManager mặc định do Spring tạo sẵn
        return authConfig.getAuthenticationManager();
    }

    @Bean // Thuật toán băm mật khẩu đảm bảo không lưu mật khẩu dạng text thuần
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
