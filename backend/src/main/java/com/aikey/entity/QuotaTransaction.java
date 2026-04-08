package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 额度流水记录实体
 *
 * <p>记录所有额度变动（消耗、充值、退款、手动调整）。
 * target_type + target_id 标识被操作的对象（virtual_key / team / project）。</p>
 */
@Entity
@Table(name = "quota_transactions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_virtual_key_id", columnList = "virtual_key_id"),
    @Index(name = "idx_target", columnList = "target_type, target_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 关联虚拟Key ID（消耗类型时存在） */
    @Column(name = "virtual_key_id")
    private Long virtualKeyId;

    /** 关联调用日志ID */
    @Column(name = "call_log_id")
    private Long callLogId;

    /** 额度类型：token / count / amount */
    @Column(name = "quota_type", length = 20, nullable = false)
    private String quotaType;

    /**
     * 交易类型：
     * consume  - 调用消耗
     * recharge - 充值
     * refund   - 退款
     * adjust   - 手动调整
     * reset    - 额度重置
     */
    @Column(name = "transaction_type", length = 20, nullable = false)
    private String transactionType;

    /**
     * 被操作对象类型：virtual_key / team / project
     */
    @Column(name = "target_type", length = 20, nullable = false)
    private String targetType;

    /** 被操作对象ID */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 本次变动量（正数=增加，负数=减少） */
    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    /** 变动前余额 */
    @Column(name = "balance_before", nullable = false, precision = 20, scale = 2)
    private BigDecimal balanceBefore;

    /** 变动后余额 */
    @Column(name = "balance_after", nullable = false, precision = 20, scale = 2)
    private BigDecimal balanceAfter;

    /** 操作说明 */
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
