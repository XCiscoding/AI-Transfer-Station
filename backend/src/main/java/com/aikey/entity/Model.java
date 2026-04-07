package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型实体类
 */
@Entity
@Table(name = "models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "model_name", length = 100, nullable = false)
    private String modelName;

    @Column(name = "model_code", length = 100, nullable = false)
    private String modelCode;

    @Column(name = "model_alias", length = 100)
    private String modelAlias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "model_type", length = 50, nullable = false)
    private String modelType;

    @Column(name = "capability_tags", columnDefinition = "TEXT")
    private String capabilityTags;

    @Column(name = "max_tokens")
    private Integer maxTokens = 4096;

    @Column(name = "input_price", precision = 10, scale = 6)
    private BigDecimal inputPrice = new BigDecimal("0.000000");

    @Column(name = "output_price", precision = 10, scale = 6)
    private BigDecimal outputPrice = new BigDecimal("0.000000");

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private Integer status = 1;

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
}
