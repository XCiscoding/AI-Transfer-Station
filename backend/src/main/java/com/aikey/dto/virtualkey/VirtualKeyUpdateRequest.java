package com.aikey.dto.virtualkey;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 虚拟Key更新请求DTO
 *
 * <p>用于更新虚拟Key的配置信息，所有字段均为可选，只更新非空字段</p>
 */
@Data
public class VirtualKeyUpdateRequest {

    /**
     * Key名称（可选）
     */
    private String keyName;

    /**
     * 团队ID（可选）
     */
    private Long teamId;

    /**
     * 项目ID（可选）
     */
    private Long projectId;

    /**
     * 允许的模型列表（JSON数组格式）
     */
    private String allowedModels;

    /**
     * 允许的模型分组ID列表
     */
    private List<Long> allowedGroupIds;

    /**
     * 额度类型（token, count, amount）
     */
    private String quotaType;

    /**
     * 额度上限
     */
    private BigDecimal quotaLimit;

    /**
     * 每分钟请求限制
     */
    private Integer rateLimitQpm;

    /**
     * 每日请求限制（0表示不限制）
     */
    private Integer rateLimitQpd;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 备注
     */
    private String remark;
}
