package com.aikey.dto.calllog;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 调用日志查询参数（通过 @RequestParam 接收）
 */
@Data
public class CallLogQueryRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    /** 模糊匹配 model_name 或 request_model */
    private String modelName;

    /** 0-失败，1-成功 */
    private Integer status;

    private Long channelId;

    private Long virtualKeyId;

    private Long userId;

    private int page = 1;

    private int size = 20;
}
