package com.aikey.service;

import com.aikey.dto.auth.LoginRequest;
import com.aikey.dto.auth.LoginResponse;
import com.aikey.dto.auth.UserInfoResponse;
import com.aikey.entity.User;
import com.aikey.repository.UserRepository;
import com.aikey.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.Collections;
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

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider tokenProvider;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        // Step 4: 使用原生SQL直接查询角色编码（绕过JPA懒加载和代理问题）
        java.util.List<String> roleCodes;
        try {
            // 直接从数据库查询角色关联
            jakarta.persistence.Query roleQuery = entityManager.createNativeQuery(
                    "SELECT r.role_code FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?");
            roleQuery.setParameter(1, user.getId());
            roleCodes = roleQuery.getResultList();

            log.info("用户 {} 角色数量(原生SQL): {}", username, roleCodes.size());

            // 如果没有角色且是admin用户，自动修复
            if (roleCodes.isEmpty() && "admin".equals(username)) {
                log.warn(">>> 用户 {} 没有角色，尝试自动修复...", username);
                try {
                    autoRepairUserRoles(user);
                    // 重新查询
                    roleCodes = entityManager.createNativeQuery(
                            "SELECT r.role_code FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?")
                            .setParameter(1, user.getId())
                            .getResultList();
                    log.info("✅ 自动修复成功，用户 {} 现在有 {} 个角色", username, roleCodes.size());
                } catch (Exception e) {
                    log.error("自动修复失败", e);
                }
            }
        } catch (Exception e) {
            log.error("查询角色失败，使用空列表", e);
            roleCodes = new java.util.ArrayList<>();
        }

        // Step 4: 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        log.info("用户登录成功: {}", username);

        // Step 5: 构建登录响应（使用原生SQL查询的roleCodes）
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleCodes)
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

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        // 安全处理roles
        java.util.List<String> roleCodes;
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleCodes = user.getRoles().stream()
                    .map(role -> role.getRoleCode())
                    .collect(Collectors.toList());
        } else {
            roleCodes = Collections.emptyList();
        }

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleCodes)
                .status(user.getStatus())
                .build();
    }

    /**
     * 自动修复用户角色关联
     *
     * <p>当用户没有角色时，自动为admin用户关联SUPER_ADMIN角色</p>
     *
     * @param user 用户对象
     */
    private void autoRepairUserRoles(User user) {
        if (!"admin".equals(user.getUsername())) {
            log.warn("非admin用户 {} 没有角色，跳过自动修复", user.getUsername());
            return;
        }

        log.info("开始为admin用户自动修复角色关联...");

        // 使用原生SQL查询SUPER_ADMIN角色ID
        jakarta.persistence.Query selectRoleQuery = entityManager.createNativeQuery(
                "SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN'");
        Object roleIdResult = selectRoleQuery.getSingleResult();

        if (roleIdResult == null) {
            log.error("SUPER_ADMIN角色不存在！无法自动修复");
            return;
        }

        long roleId = ((Number) roleIdResult).longValue();
        log.info("找到SUPER_ADMIN角色，ID: {}", roleId);

        // 插入user_roles关联
        jakarta.persistence.Query insertQuery = entityManager.createNativeQuery(
                "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, ?)");
        insertQuery.setParameter(1, user.getId());
        insertQuery.setParameter(2, roleId);
        int rows = insertQuery.executeUpdate();

        if (rows > 0) {
            log.info("✅ 成功为admin用户关联SUPER_ADMIN角色 (userId={}, roleId={})", user.getId(), roleId);
        } else {
            log.info("admin用户已有角色关联或插入失败（影响行数=0）");
        }
    }
}
