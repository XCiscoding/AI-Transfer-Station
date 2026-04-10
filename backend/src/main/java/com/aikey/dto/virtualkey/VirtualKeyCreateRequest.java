package com.aikey.dto.virtualkey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 虚拟Key创建请求DTO
 *
 * <p>用于接收虚拟Key的创建请求参数，包含Key基本信息、额度配置、限速配置等</p>
 */
@Data
public class VirtualKeyCreateRequest {

    @NotBlank(message = "Key名称不能为空")
    private String keyName;

    @NotNull(message = "所属用户不能为空")
    private Long userId;

    /**
     * 团队ID
     */
    @NotNull(message = "团队ID不能为空")
    private Long teamId;

    /**
     * 项目ID
     */
    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    /**
     * 允许的模型列表（JSON数组格式，空表示不限制）
     * <p>示例: ["gpt-4", "gpt-3.5-turbo"]</p>
     */
    private String allowedModels;

    /**
     * 允许的模型分组ID列表，仅允许单个分组
     */
    @NotEmpty(message = "请选择模型分组")
    private List<Long> allowedGroupIds;

    /**
     * 指定路由渠道ID
     */
    @NotNull(message = "路由渠道不能为空")
    private Long channelId;

    @NotBlank(message = "额度类型不能为空")
    private String quotaType;  // token, count, amount

    @NotNull(message = "额度上限不能为空")
    private BigDecimal quotaLimit;

    /**
     * 每分钟请求限制（默认60次）
     */
    private Integer rateLimitQpm = 60;

    /**
     * 每日请求限制（0表示不限制）
     */
    private Integer rateLimitQpd = 0;

    /**
     * 过期时间（可选，空表示永不过期）
     */
    private LocalDateTime expireTime;

    /**
     * 备注
     */
    private String remark;
}
