package com.aikey.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 *
 * <p>提供代理网关专用的RestTemplate Bean，配置合理的超时参数。
 * LLM推理耗时较长，读超时设置为120秒。</p>
 */
@Configuration
public class RestTemplateConfig {

    @Bean(name = "proxyRestTemplate")
    public RestTemplate proxyRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        return new RestTemplate(factory);
    }
}
