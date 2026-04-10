package com.aikey.dto.team;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TeamMemberVO {

    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Boolean owner;
    private LocalDateTime joinedAt;
}
