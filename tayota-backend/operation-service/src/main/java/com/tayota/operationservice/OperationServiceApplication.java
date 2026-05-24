package com.tayota.operationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Bật chế độ xử lý bất đồng bộ cho các tác vụ như gửi email, nhắc nhỏ, v.v.
@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class OperationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperationServiceApplication.class, args);
    }

}
