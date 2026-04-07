package com.aikey.dto.team;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeamUpdateRequest {

    private String teamName;

    private String description;

    private BigDecimal quotaLimit;

    private BigDecimal quotaWeight;
}
