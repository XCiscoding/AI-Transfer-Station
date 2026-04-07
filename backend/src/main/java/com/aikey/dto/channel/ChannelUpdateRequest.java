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
}
