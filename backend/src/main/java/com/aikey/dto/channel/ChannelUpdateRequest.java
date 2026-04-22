package com.aikey.dto.channel;

import lombok.Data;

/**
 * 渠道更新请求DTO
 */
@Data
public class ChannelUpdateRequest {

    private String channelName;

    private String baseUrl;

    private String apiKey;         // 如果更新则重新加密

    private Integer weight;

    private Integer priority;

    private String remark;

    private Integer status;

    // 扩展配置字段（存储在 config JSON 列）
    private String provider;

    private String apiVersion;

    private Integer maxTokens;

    private Integer maxRpm;

    private Integer maxTpm;

    private Integer timeout;
}
