package com.aikey.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 文档配置类
 *
 * <p>配置API文档信息和JWT Bearer Token认证支持</p>
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置OpenAPI文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI调度中心 - 企业API Key管理系统 API")
                        .description("""
                                企业级AI模型API Key统一管理平台
                                
                                ## 功能特性
                                - **渠道管理**：管理多个AI服务提供商的接入渠道
                                - **Key管理**：真实Key与虚拟Key的全生命周期管理
                                - **额度控制**：Token/次数/金额多维度额度管控
                                - **Auto模式**：智能模型选择与负载均衡
                                - **日志审计**：完整的调用链路追踪与日志记录
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Key Management Team")
                                .email("support@aikey.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer Token 认证")));
    }
}
