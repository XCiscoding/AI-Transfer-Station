package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调用日志实体类
 *
 * <p>记录每次API调用的关键信息，包括请求方、渠道、模型、Token消耗、耗时、状态等。
 * 对应数据库中已有的call_logs表，仅追加写入，不支持更新和软删除。</p>
 */
@Entity
@Table(name = "call_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "trace_id", length = 64, nullable = false, unique = true)
    private String traceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "virtual_key_id")
    private Long virtualKeyId;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "request_type", length = 50, nullable = false)
    private String requestType;

    @Column(name = "request_model", length = 100, nullable = false)
    private String requestModel;

    @Column(name = "is_auto_mode", nullable = false, columnDefinition = "TINYINT")
    private Integer isAutoMode = 0;

    @Column(name = "selected_model", length = 100)
    private String selectedModel;

    @Column(name = "selection_strategy", length = 50)
    private String selectionStrategy;

    @Column(name = "prompt_tokens")
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens")
    private Integer completionTokens = 0;

    @Column(name = "total_tokens")
    private Integer totalTokens = 0;

    @Column(name = "cost", precision = 10, scale = 6)
    private BigDecimal cost = new BigDecimal("0.000000");

    @Column(name = "response_time")
    private Integer responseTime = 0;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private Integer status;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
