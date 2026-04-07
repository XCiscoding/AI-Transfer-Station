package com.aikey.dto.channel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 渠道创建请求DTO
 */
@Data
public class ChannelCreateRequest {

    @NotBlank(message = "渠道名称不能为空")
    private String channelName;

    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @NotBlank(message = "渠道类型不能为空")
    private String channelType;   // openai/qwen/wenxin/doubao/claude/gemini/deepseek

    @NotBlank(message = "Base URL不能为空")
    private String baseUrl;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;        // 明文，后端自动AES加密

    private Integer weight = 100;

    private Integer priority = 0;

    private String remark;
}
