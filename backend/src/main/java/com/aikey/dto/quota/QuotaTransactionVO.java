package com.aikey.dto.quota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 额度流水视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaTransactionVO {

    private Long id;
    private Long userId;
    private Long virtualKeyId;
    private Long callLogId;
    private String quotaType;
    private String transactionType;
    private String targetType;
    private Long targetId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;
}
