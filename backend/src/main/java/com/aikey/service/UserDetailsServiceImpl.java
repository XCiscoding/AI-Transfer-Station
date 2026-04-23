package com.aikey.service;

import com.aikey.entity.Role;
import com.aikey.entity.User;
import com.aikey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户详情服务实现类
 *
 * <p>实现Spring Security的UserDetailsService接口，
 * 根据用户名加载用户信息和权限</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 根据用户名加载用户详情
     *
     * @param username 用户名
     * @return UserDetails Spring Security用户详情对象
     * @throws UsernameNotFoundException 用户不存在时抛出异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("正在加载用户信息: {}", username);

        // 1. 显式加载角色，避免 @PreAuthorize 使用的 SecurityContext 丢失 ROLE_*。
        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 2. 获取用户权限列表
        Set<Role> roles = user.getRoles();
        List<SimpleGrantedAuthority> authorities;
        if (roles != null && !roles.isEmpty()) {
            authorities = getAuthorities(roles);
        } else {
            // 如果用户没有关联角色，提供默认角色以避免认证失败
            // 这通常意味着数据初始化有问题（data.sql未正确执行）
            log.warn("用户 {} 没有关联角色，使用默认角色 USER", username);
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        log.info("用户 {} 加载成功, 角色数量: {}, 权限数量: {}",
                username,
                roles != null ? roles.size() : 0,
                authorities.size());

        // 3. 返回Spring Security的User对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    /**
     * 将角色集合转换为权限列表
     *
     * @param roles 角色集合
     * @return 权限列表（格式：ROLE_角色代码）
     */
    private List<SimpleGrantedAuthority> getAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()))
                .collect(Collectors.toList());
    }
}
