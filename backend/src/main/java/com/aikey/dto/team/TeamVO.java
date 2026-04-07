package com.aikey.dto.team;

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
public class TeamVO {

    private Long id;
    private String teamName;
    private String teamCode;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;
    private BigDecimal quotaLimit;
    private BigDecimal quotaUsed;
    private BigDecimal quotaRemaining;
    private BigDecimal quotaWeight;
    private Integer status;
    private LocalDateTime createdAt;
}
