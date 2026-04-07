package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目实体类
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "project_name", length = 100, nullable = false)
    private String projectName;

    @Column(name = "project_code", length = 50, nullable = false, unique = true)
    private String projectCode;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Builder.Default
    @Column(name = "quota_limit", nullable = false)
    private BigDecimal quotaLimit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "quota_used", nullable = false)
    private BigDecimal quotaUsed = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "quota_remaining", nullable = false)
    private BigDecimal quotaRemaining = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private Integer status = 1;

    @Builder.Default
    @Column(name = "quota_weight", nullable = false)
    private BigDecimal quotaWeight = BigDecimal.ONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    private Integer deleted = 0;
}
