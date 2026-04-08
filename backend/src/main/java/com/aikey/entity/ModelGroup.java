package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 模型分组实体类
 */
@Entity
@Table(name = "model_groups", uniqueConstraints = {
    @UniqueConstraint(columnNames = "group_name")
}, indexes = {
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;

    @Column(name = "description", length = 500)
    private String description;

    /** 模型ID列表，存储为JSON数组字符串，如 [1,2,3] */
    @Column(name = "model_ids", columnDefinition = "TEXT", nullable = false)
    private String modelIds;

    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private Integer status = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    private Integer deleted = 0;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
