package com.aikey.dto.quota;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 额度调整请求（amount 正数=增加，负数=减少）
 */
@Data
public class QuotaAdjustRequest {

    @NotBlank(message = "目标类型不能为空")
    private String targetType;  // virtual_key / team / project

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @NotNull(message = "调整量不能为空")
    private BigDecimal amount;  // 正=增加，负=减少

    private String description;
}
