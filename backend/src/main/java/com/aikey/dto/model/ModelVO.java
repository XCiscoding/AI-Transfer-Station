package com.aikey.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型VO（列表展示用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVO {

    private Long id;

    private String modelName;

    private String modelCode;

    private String modelAlias;

    private Long channelId;

    private String channelName;

    private String modelType;

    private String capabilityTags;

    private Integer maxTokens;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private BigDecimal quotaWeight;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;
}
