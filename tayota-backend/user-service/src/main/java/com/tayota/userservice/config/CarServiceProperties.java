package com.tayota.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "car-service")
public class CarServiceProperties {
    private String host;
    private int port;

    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}