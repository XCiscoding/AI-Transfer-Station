package com.aikey.dto.virtualkey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 虚拟Key展示VO
 *
 * <p>用于列表展示和详情展示，包含关联的用户信息、额度使用情况等</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualKeyVO {

    private Long id;

    /**
     * Key名称
     */
    private String keyName;

    /**
     * Key值（完整显示，因为是虚拟Key而非真实密钥）
     */
    private String keyValue;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 用户名（关联查询）
     */
    private String userName;

    /**
     * 团队ID
     */
    private Long teamId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 允许的模型列表（JSON数组格式）
     */
    private String allowedModels;

    /**
     * 额度类型（token, count, amount）
     */
    private String quotaType;

    /**
     * 额度上限
     */
    private BigDecimal quotaLimit;

    /**
     * 已使用额度
     */
    private BigDecimal quotaUsed;

    /**
     * 剩余额度
     */
    private BigDecimal quotaRemaining;

    /**
     * 每分钟请求限制
     */
    private Integer rateLimitQpm;

    /**
     * 每日请求限制
     */
    private Integer rateLimitQpd;

    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
