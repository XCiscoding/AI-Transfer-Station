package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", length = 100, nullable = false)
    private String ruleName;

    @Column(name = "rule_type", length = 50, nullable = false)
    private String ruleType;

    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "condition_config", nullable = false, columnDefinition = "TEXT")
    private String conditionConfig;

    @Column(name = "action_config", nullable = false, columnDefinition = "TEXT")
    private String actionConfig;

    @Column(name = "notification_channels", length = 255)
    private String notificationChannels;

    @Column(name = "is_enabled", nullable = false, columnDefinition = "TINYINT")
    @Builder.Default
    private Integer isEnabled = 1;

    @Column(name = "last_triggered_time")
    private LocalDateTime lastTriggeredTime;

    @Column(name = "trigger_count", nullable = false)
    @Builder.Default
    private Integer triggerCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT")
    @Builder.Default
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
