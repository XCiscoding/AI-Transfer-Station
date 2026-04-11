package com.aikey.service;

import com.aikey.config.DataInitializer;
import com.aikey.dto.auth.LoginRequest;
import com.aikey.dto.auth.LoginResponse;
import com.aikey.dto.auth.UserInfoResponse;
import com.aikey.entity.User;
import com.aikey.repository.TeamRepository;
import com.aikey.repository.UserRepository;
import com.aikey.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务类
 *
 * <p>提供用户登录、获取用户信息等认证相关业务逻辑</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider tokenProvider;

    private final UserRepository userRepository;

    private final TeamRepository teamRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 用户登录
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>使用AuthenticationManager进行身份认证</li>
     *   <li>生成JWT Token</li>
     *   <li>更新用户最后登录时间</li>
     *   <li>构建并返回登录响应</li>
     * </ol>
     *
     * @param request 登录请求（包含用户名和密码）
     * @return 登录响应（包含Token和用户信息）
     */
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        log.info("用户尝试登录: {}", username);

        // Step 1: 执行身份认证
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
            log.info("身份认证成功: {}", username);
        } catch (BadCredentialsException e) {
            log.warn("身份认证失败 - 用户名或密码错误: {}", username);
            throw new RuntimeException("用户名或密码错误", e);
        } catch (Exception e) {
            log.error("身份认证失败 - 异常类型: {}", e.getClass().getSimpleName(), e);
            throw new RuntimeException("认证失败: " + e.getMessage(), e);
        }

        // Step 2: 生成JWT Token
        String token = tokenProvider.generateToken((UserDetails) authentication.getPrincipal());

        // Step 3: 查询用户完整信息（使用JPQL显式JOIN加载roles，确保不出现空集合）
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        // Step 4: 使用原生SQL直接查询角色编码（绕过JPA懒加载和代理问题）
        java.util.List<String> roleCodes;
        try {
            roleCodes = queryRoleCodes(user.getId());

            log.info("用户 {} 角色数量(原生SQL): {}", username, roleCodes.size());

            if (roleCodes.isEmpty() && isBootstrapSuperAdmin(user.getUsername())) {
                log.warn(">>> 引导超级管理员用户 {} 没有角色，尝试自动修复...", username);
                try {
                    autoRepairUserRoles(user);
                    roleCodes = queryRoleCodes(user.getId());
                    log.info("✅ 自动修复成功，用户 {} 现在有 {} 个角色", username, roleCodes.size());
                } catch (Exception e) {
                    log.error("自动修复失败", e);
                }
            }
        } catch (Exception e) {
            log.error("查询角色失败，使用空列表", e);
            roleCodes = new ArrayList<>();
        }

        // Step 4: 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        boolean isSuperAdmin = roleCodes.contains(SUPER_ADMIN_ROLE);
        boolean isTeamOwner = user.getId() != null && teamRepository.existsByOwnerIdAndDeleted(user.getId(), 0);

        log.info("用户登录成功: {}", username);

        // Step 5: 构建登录响应（使用原生SQL查询的roleCodes）
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleCodes)
                .isSuperAdmin(isSuperAdmin)
                .isTeamOwner(isTeamOwner)
                .status(user.getStatus())
                .build();
    }

    /**
     * 获取当前用户信息
     *
     * @param username 用户名
     * @return 用户信息响应
     */
    public UserInfoResponse getUserInfo(String username) {
        log.debug("获取用户信息: {}", username);

        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        // 直接用原生SQL查角色，避免JPA关联状态影响 /auth/me 返回
        List<String> roleCodes = queryRoleCodes(user.getId());

        boolean isSuperAdmin = roleCodes.contains(SUPER_ADMIN_ROLE);
        boolean isTeamOwner = user.getId() != null && teamRepository.existsByOwnerIdAndDeleted(user.getId(), 0);

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleCodes)
                .isSuperAdmin(isSuperAdmin)
                .isTeamOwner(isTeamOwner)
                .status(user.getStatus())
                .build();
    }

    /**
     * 自动修复用户角色关联
     *
     * <p>当引导超级管理员用户没有角色时，自动为其关联SUPER_ADMIN角色</p>
     *
     * @param user 用户对象
     */
    private void autoRepairUserRoles(User user) {
        if (!isBootstrapSuperAdmin(user.getUsername())) {
            log.warn("非引导超级管理员用户 {} 没有角色，跳过自动修复", user.getUsername());
            return;
        }

        log.info("开始为引导超级管理员用户 {} 自动修复角色关联...", user.getUsername());

        jakarta.persistence.Query selectRoleQuery = entityManager.createNativeQuery(
                "SELECT id FROM roles WHERE role_code = '" + SUPER_ADMIN_ROLE + "'");
        Object roleIdResult = selectRoleQuery.getSingleResult();

        if (roleIdResult == null) {
            log.error("SUPER_ADMIN角色不存在！无法自动修复");
            return;
        }

        long roleId = ((Number) roleIdResult).longValue();
        log.info("找到SUPER_ADMIN角色，ID: {}", roleId);

        jakarta.persistence.Query insertQuery = entityManager.createNativeQuery(
                "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, ?)");
        insertQuery.setParameter(1, user.getId());
        insertQuery.setParameter(2, roleId);
        int rows = insertQuery.executeUpdate();

        if (rows > 0) {
            log.info("✅ 成功为用户 {} 关联SUPER_ADMIN角色 (userId={}, roleId={})", user.getUsername(), user.getId(), roleId);
        } else {
            log.info("用户 {} 已有角色关联或插入失败（影响行数=0）", user.getUsername());
        }
    }

    private boolean isBootstrapSuperAdmin(String username) {
        return DataInitializer.BOOTSTRAP_SUPER_ADMIN_USERNAMES.contains(username);
    }

    private List<String> queryRoleCodes(Long userId) {
        @SuppressWarnings("unchecked")
        List<Object> rawRoleCodes = entityManager.createNativeQuery(
                        "SELECT r.role_code FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?")
                .setParameter(1, userId)
                .getResultList();
        return rawRoleCodes.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }
}
