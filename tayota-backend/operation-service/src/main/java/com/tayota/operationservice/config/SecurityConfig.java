package com.tayota.operationservice.config;

import com.tayota.commoncore.filter.HeaderAuthenticationFilter;
import com.tayota.operationservice.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;

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
