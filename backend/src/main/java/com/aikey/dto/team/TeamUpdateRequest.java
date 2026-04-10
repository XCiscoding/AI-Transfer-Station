package com.aikey.dto.team;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TeamUpdateRequest {

    private String teamName;

    private String description;

    private Long ownerId;

    private BigDecimal quotaLimit;

    private BigDecimal quotaWeight;

    private List<Long> allowedGroupIds;
}
