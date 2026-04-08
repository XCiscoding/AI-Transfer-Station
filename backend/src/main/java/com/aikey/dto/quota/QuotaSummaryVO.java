package com.aikey.dto.quota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 额度余额摘要视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaSummaryVO {

    private String targetType;
    private Long targetId;
    private String targetName;
    private BigDecimal quotaLimit;
    private BigDecimal quotaUsed;
    private BigDecimal quotaRemaining;
    private String quotaType;
}
