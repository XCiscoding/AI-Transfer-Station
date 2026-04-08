package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 虚拟API Key实体类
 *
 * <p>存储虚拟Key信息，采用sk-xxx格式，用于对外提供API访问凭证。
 * 虚拟Key与用户绑定，支持配额限制、速率限制等策略配置。</p>
 */
@Entity
@Table(name = "virtual_keys", uniqueConstraints = {
    @UniqueConstraint(columnNames = "key_value")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "key_name", length = 100, nullable = false)
    private String keyName;

    @Column(name = "key_value", length = 100, nullable = false, unique = true)
    private String keyValue;  // sk-xxx格式

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "allowed_models", columnDefinition = "TEXT")
    private String allowedModels;  // JSON数组，空表示不限制

    @Column(name = "allowed_group_ids", columnDefinition = "TEXT")
    private String allowedGroupIds;  // 模型分组ID的JSON数组

    @Column(name = "quota_type", length = 20, nullable = false)
    private String quotaType;  // token, count, amount

    @Column(name = "quota_limit", nullable = false)
    private BigDecimal quotaLimit;

    @Column(name = "quota_used", nullable = false)
    private BigDecimal quotaUsed;

    @Column(name = "quota_remaining", nullable = false)
    private BigDecimal quotaRemaining;

    @Column(name = "rate_limit_qpm")
    private Integer rateLimitQpm;  // 每分钟请求限制

    @Column(name = "rate_limit_qpd")
    private Integer rateLimitQpd;  // 每日请求限制（0表示不限制）

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer status;  // 0-禁用，1-启用

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "last_used_time")
    private LocalDateTime lastUsedTime;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    private Integer deleted = 0;
}
