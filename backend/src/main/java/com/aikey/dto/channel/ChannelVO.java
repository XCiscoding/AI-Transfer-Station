package com.aikey.dto.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 渠道VO（列表展示用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelVO {

    private Long id;

    private String channelName;

    private String channelCode;

    private String channelType;

    private String baseUrl;

    private String apiKeyMask;     // sk-***...***abc 掩码格式

    private Integer weight;

    private Integer priority;

    private Integer status;

    private Integer healthStatus;

    private LocalDateTime healthCheckTime;

    private LocalDateTime createdAt;

    // 扩展配置字段（来自 config JSON 列）
    private String provider;

    private String apiVersion;

    private Integer maxTokens;

    private Integer maxRpm;

    private Integer maxTpm;

    private Integer timeout;
}
