package com.tayota.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// Bật chế độ xử lý bất đồng bộ cho các tác vụ như gửi email, nhắc nhỏ, v.v.
@EnableAsync
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
