package com.aikey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI调度中心 - 企业API Key管理系统
 * Spring Boot 启动类
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AiKeyManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiKeyManagementApplication.class, args);
    }
}
