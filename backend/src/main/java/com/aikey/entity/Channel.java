package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 渠道实体类
 */
@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "channel_name", length = 100, nullable = false)
    private String channelName;

    @Column(name = "channel_code", length = 50, nullable = false, unique = true)
    private String channelCode;

    @Column(name = "channel_type", length = 50, nullable = false)
    private String channelType;

    @Column(name = "base_url", length = 255, nullable = false)
    private String baseUrl;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "models", columnDefinition = "TEXT")
    private String models;

    @Column(name = "weight", nullable = false)
    private Integer weight = 100;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private Integer status = 1;

    @Column(name = "health_status", nullable = false, columnDefinition = "TINYINT")
    private Integer healthStatus = 1;

    @Column(name = "health_check_time")
    private LocalDateTime healthCheckTime;

    @Column(name = "success_count")
    private Long successCount = 0L;

    @Column(name = "fail_count")
    private Long failCount = 0L;

    @Column(name = "avg_response_time")
    private Integer avgResponseTime = 0;

    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    private Integer deleted = 0;

    /**
     * 渠道与模型一对多关系
     */
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Model> modelSet = new HashSet<>();
}
