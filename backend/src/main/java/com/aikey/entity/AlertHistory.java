package com.aikey.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "alert_type", length = 50, nullable = false)
    private String alertType;

    @Column(name = "alert_level", length = 20, nullable = false)
    private String alertLevel;

    @Column(name = "alert_title", length = 255, nullable = false)
    private String alertTitle;

    @Column(name = "alert_content", columnDefinition = "TEXT")
    private String alertContent;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "notification_status", length = 20)
    @Builder.Default
    private String notificationStatus = "pending";

    @Column(name = "notification_time")
    private LocalDateTime notificationTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
