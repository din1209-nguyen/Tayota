package com.tayota.commoncore.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
// Spring quét toàn bộ thư mục chung để nhặt các Bean
@ComponentScan(basePackages = "com.tayota.commoncore")
// Class này đóng vai trò đánh dấu và ra lệnh scan
public class CommonCoreAutoConfiguration {}