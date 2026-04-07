package com.aikey.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeamCreateRequest {

    @NotBlank(message = "团队名称不能为空")
    private String teamName;

    @NotBlank(message = "团队编码不能为空")
    private String teamCode;

    private String description;

    @NotNull(message = "所有者ID不能为空")
    private Long ownerId;

    private BigDecimal quotaLimit = BigDecimal.ZERO;

    private BigDecimal quotaWeight = BigDecimal.ONE;
}
