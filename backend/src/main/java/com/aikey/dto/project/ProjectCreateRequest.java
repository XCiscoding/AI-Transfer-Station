package com.aikey.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @NotBlank(message = "项目编码不能为空")
    private String projectCode;

    private String description;

    private Long teamId;

    @NotNull(message = "所有者ID不能为空")
    private Long ownerId;

    private BigDecimal quotaLimit = BigDecimal.ZERO;

    private BigDecimal quotaWeight = BigDecimal.ONE;
}
