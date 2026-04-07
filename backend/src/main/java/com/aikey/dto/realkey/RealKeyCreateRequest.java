package com.aikey.dto.realkey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 真实Key录入请求DTO
 */
@Data
public class RealKeyCreateRequest {

    @NotBlank(message = "Key名称不能为空")
    private String keyName;

    @NotBlank(message = "Key值不能为空")
    private String keyValue;        // 明文，后端自动AES加密

    @NotNull(message = "渠道ID不能为空")
    private Long channelId;

    private LocalDateTime expireTime;

    private String remark;
}
