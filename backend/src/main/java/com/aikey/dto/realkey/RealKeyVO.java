package com.aikey.dto.realkey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 真实Key展示VO（列表展示用，含掩码显示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealKeyVO {

    private Long id;

    private String keyName;

    private String keyMask;         // sk-***...***abc 掩码格式

    private Long channelId;

    private String channelName;     // 关联的渠道名称

    private Integer status;         // 0-禁用, 1-启用

    private LocalDateTime expireTime;

    private Long usageCount;

    private LocalDateTime lastUsedTime;

    private LocalDateTime createdAt;

    private String baseUrl;    // Key 级别接口地址
}
