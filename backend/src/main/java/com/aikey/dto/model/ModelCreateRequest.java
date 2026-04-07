package com.aikey.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建模型请求DTO
 */
@Data
public class ModelCreateRequest {

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "模型编码不能为空")
    private String modelCode;

    private String modelAlias;

    @NotNull(message = "渠道ID不能为空")
    private Long channelId;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    private String capabilityTags;

    private Integer maxTokens = 4096;

    private BigDecimal inputPrice = BigDecimal.ZERO;

    private BigDecimal outputPrice = BigDecimal.ZERO;

    private String remark;
}
