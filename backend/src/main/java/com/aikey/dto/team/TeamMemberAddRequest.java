package com.aikey.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamMemberAddRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
