package com.aikey.dto.team;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeamCreateRequest {

    private String teamName;

    private String teamCode;

    private String description;

    private Long ownerId;

    private BigDecimal quotaLimit = BigDecimal.ZERO;

    private BigDecimal quotaWeight = BigDecimal.ONE;
}
