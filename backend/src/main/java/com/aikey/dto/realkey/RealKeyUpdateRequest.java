package com.aikey.dto.realkey;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 真实Key更新请求DTO
 */
@Data
public class RealKeyUpdateRequest {

    private String keyName;

    private String keyValue;         // 如果更新则重新加密

    private LocalDateTime expireTime;

    private String remark;

    private String baseUrl;    // 覆盖渠道默认地址，可为空
}
