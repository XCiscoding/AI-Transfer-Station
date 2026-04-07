package com.aikey.dto.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVO {

    private Long id;
    private String projectName;
    private String projectCode;
    private String description;
    private Long teamId;
    private String teamName;
    private Long ownerId;
    private String ownerName;
    private BigDecimal quotaLimit;
    private BigDecimal quotaUsed;
    private BigDecimal quotaRemaining;
    private BigDecimal quotaWeight;
    private Integer status;
    private LocalDateTime createdAt;
}
