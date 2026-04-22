package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 真实API Key实体类
 *
 * <p>存储加密后的真实API Key信息，
 * 通过AES-256-GCM加密保护敏感数据</p>
 */
@Entity
@Table(name = "real_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "key_name", length = 100, nullable = false)
    private String keyName;

    @Column(name = "key_value_encrypted", columnDefinition = "TEXT", nullable = false)
    private String keyValueEncrypted;

    @Column(name = "key_mask", length = 50, nullable = false)
    private String keyMask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private Integer status;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Column(name = "last_used_time")
    private LocalDateTime lastUsedTime;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    private Integer deleted = 0;
}
