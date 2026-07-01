package com.tayota.operationservice.config;

import com.cloudinary.Cloudinary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(Map.of(
                "cloud_name", nullToEmpty(properties.getCloudName()),
                "api_key", nullToEmpty(properties.getApiKey()),
                "api_secret", nullToEmpty(properties.getApiSecret())
        ));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
