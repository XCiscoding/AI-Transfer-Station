package com.aikey.dto.calllog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调用日志视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallLogVO {

    private Long id;
    private String traceId;
    private Long userId;
    private Long virtualKeyId;
    private Long channelId;
    private Long modelId;
    private String modelName;
    private String requestModel;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    /** 加权消耗量（暂时留null，后续关联流水表填充） */
    private BigDecimal weightedAmount;
    private Integer responseTime;
    private Integer status;
    private String errorCode;
    private String errorMessage;
    private String clientIp;
    private LocalDateTime createdAt;
    /** 渠道名称（冗余，查询时补充） */
    private String channelName;
}
