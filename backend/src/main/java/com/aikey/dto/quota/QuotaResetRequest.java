package com.aikey.dto.quota;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 额度重置请求（清零已用，剩余=上限）
 */
@Data
public class QuotaResetRequest {

    @NotBlank(message = "目标类型不能为空")
    private String targetType;  // virtual_key / team / project

    @NotNull(message = "目标ID不能为空")
    private Long targetId;
}
