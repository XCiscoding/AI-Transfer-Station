package com.aikey.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String tokenType = "Bearer";

    private Long userId;

    private String username;

    private String email;

    private List<String> roles;

    private Boolean isSuperAdmin;

    private Boolean isTeamOwner;

    private Integer status;
}
