package com.aikey.controller;

import com.aikey.dto.auth.LoginRequest;
import com.aikey.dto.auth.LoginResponse;
import com.aikey.dto.auth.UserInfoResponse;
import com.aikey.dto.common.Result;
import com.aikey.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 认证管理控制器
 *
 * <p>提供用户登录、获取当前用户信息等认证相关接口</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * <p>通过用户名和密码登录，成功后返回JWT Token</p>
     *
     * @param request 登录请求（包含用户名和密码）
     * @return 统一响应格式（包含Token和用户基本信息）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码登录获取JWT Token")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>根据JWT Token中的用户信息查询并返回当前用户的详细信息</p>
     *
     * @param userDetails 当前认证用户详情（从SecurityContext中自动注入）
     * @return 统一响应格式（包含用户详细信息）
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "根据Token获取当前登录用户信息")
    public Result<UserInfoResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        // 防御性检查：如果userDetails为null，说明Token无效或认证未通过
        if (userDetails == null) {
            return Result.error(401, "未登录或Token已过期");
        }
        return Result.success(authService.getUserInfo(userDetails.getUsername()));
    }
}
