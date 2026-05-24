package com.tayota.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {
    @Test
    void allowsConfiguredFrontendOriginsWithCredentials() {
        CorsConfiguration configuration = new CorsConfig("http://localhost:3000,https://tayota.com")
                .buildCorsConfiguration();

        assertThat(configuration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configuration.checkOrigin("https://tayota.com")).isEqualTo("https://tayota.com");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void rejectsUnknownFrontendOrigin() {
        CorsConfiguration configuration = new CorsConfig("http://localhost:3000").buildCorsConfiguration();

        assertThat(configuration.checkOrigin("https://unknown.com")).isNull();
    }
}
