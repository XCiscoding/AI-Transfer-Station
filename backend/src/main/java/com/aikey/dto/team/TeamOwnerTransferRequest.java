package com.aikey.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamOwnerTransferRequest {

    @NotNull(message = "新团队管理员ID不能为空")
    private Long newOwnerId;
}
