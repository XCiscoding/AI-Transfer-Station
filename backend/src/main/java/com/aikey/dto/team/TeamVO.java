package com.aikey.dto.team;

import com.aikey.dto.modelgroup.ModelGroupVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<Long> allowedGroupIds;
    private List<ModelGroupVO> allowedGroups;
    private LocalDateTime createdAt;
}
