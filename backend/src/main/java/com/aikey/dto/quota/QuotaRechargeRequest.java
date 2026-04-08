package com.aikey.dto.quota;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 额度充值请求
 */
@Data
public class QuotaRechargeRequest {

    @NotBlank(message = "目标类型不能为空")
    private String targetType;  // virtual_key / team / project

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0.01")
    private BigDecimal amount;

    private String description;
}
