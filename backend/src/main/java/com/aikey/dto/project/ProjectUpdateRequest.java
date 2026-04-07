package com.aikey.dto.project;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectUpdateRequest {

    private String projectName;

    private String description;

    private Long teamId;

    private BigDecimal quotaLimit;

    private BigDecimal quotaWeight;
}
