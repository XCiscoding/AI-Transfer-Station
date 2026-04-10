package com.aikey.service;

import com.aikey.dto.user.UserCreateRequest;
import com.aikey.dto.user.UserCreateResponse;
import com.aikey.entity.Role;
import com.aikey.entity.User;
import com.aikey.exception.BusinessException;
import com.aikey.repository.RoleRepository;
import com.aikey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCreateResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已存在");
        }

        Role defaultRole = roleRepository.findByRoleCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new BusinessException("默认角色不存在"));

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .realName(request.getRealName())
                .status(1)
                .isLocked(0)
                .loginFailCount(0)
                .deleted(0)
                .createdAt(now)
                .updatedAt(now)
                .roles(Set.of(defaultRole))
                .build();

        User savedUser = userRepository.save(user);
        return UserCreateResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .realName(savedUser.getRealName())
                .roles(List.of(defaultRole.getRoleCode()))
                .build();
    }
}
