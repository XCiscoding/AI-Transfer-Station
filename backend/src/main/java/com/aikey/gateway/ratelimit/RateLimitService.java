package com.aikey.gateway.ratelimit;

import com.aikey.entity.VirtualKey;
import com.aikey.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 限流服务
 *
 * <p>基于Redis INCR+TTL实现QPM（每分钟请求数）和QPD（每日请求数）限流。
 * Redis不可用时fail-open（放行请求，记录警告日志）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查限流，超限则抛出429异常
     *
     * @param virtualKey 虚拟Key
     */
    public void checkRateLimit(VirtualKey virtualKey) {
        try {
            // QPM限流
            if (virtualKey.getRateLimitQpm() != null && virtualKey.getRateLimitQpm() > 0) {
                String qpmKey = "rate:qpm:" + virtualKey.getId();
                Long count = redisTemplate.opsForValue().increment(qpmKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(qpmKey, Duration.ofSeconds(60));
                }
                if (count != null && count > virtualKey.getRateLimitQpm()) {
                    throw new BusinessException(429, "Rate limit exceeded: " + virtualKey.getRateLimitQpm() + " requests per minute.");
                }
            }

            // QPD限流
            if (virtualKey.getRateLimitQpd() != null && virtualKey.getRateLimitQpd() > 0) {
                String qpdKey = "rate:qpd:" + virtualKey.getId();
                Long count = redisTemplate.opsForValue().increment(qpdKey);
                if (count != null && count == 1) {
                    // TTL设为到当天午夜的剩余秒数
                    long secondsUntilMidnight = Duration.between(
                            LocalDateTime.now(),
                            LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
                    ).getSeconds();
                    redisTemplate.expire(qpdKey, Duration.ofSeconds(Math.max(secondsUntilMidnight, 1)));
                }
                if (count != null && count > virtualKey.getRateLimitQpd()) {
                    throw new BusinessException(429, "Rate limit exceeded: " + virtualKey.getRateLimitQpd() + " requests per day.");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis不可用时放行
            log.warn("Redis限流检查异常，放行请求: {}", e.getMessage());
        }
    }
}
