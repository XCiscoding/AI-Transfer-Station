package com.aikey.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置类
 *
 * <p>配置RedisTemplate使用String序列化器，
 * 避免默认JDK序列化导致的乱码问题</p>
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * 使用String序列化器处理Key和Value
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用String序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Key使用String序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value使用String序列化
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
