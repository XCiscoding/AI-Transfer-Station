package com.aikey.controller;

import com.aikey.entity.User;
import com.aikey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 健康检查控制器
 *
 * <p>提供系统健康状态检查接口</p>
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final UserRepository userRepository;

    /**
     * 健康检查接口
     * 返回服务运行状态
     *
     * @return 健康状态信息
     */
    @GetMapping("/actuator/health")
    public String health() {
        return "{\"status\":\"UP\"}";
    }

    /**
     * 临时调试接口 - 查看admin用户信息
     * TODO: 修复完成后删除此接口
     */
    @GetMapping("/debug/admin-user")
    public String debugAdminUser() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Admin User Debug Info ===\n");

        Optional<User> userOpt = userRepository.findByUsername("admin");
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            sb.append("User ID: ").append(user.getId()).append("\n");
            sb.append("Username: ").append(user.getUsername()).append("\n");
            sb.append("Password (first 20 chars): ").append(user.getPassword().substring(0, Math.min(20, user.getPassword().length()))).append("\n");
            sb.append("Password length: ").append(user.getPassword().length()).append("\n");
            sb.append("Roles count: ").append(user.getRoles().size()).append("\n");
            sb.append("Roles: ").append(user.getRoles()).append("\n");
        } else {
            sb.append("Admin user NOT FOUND in database!");
        }

        return sb.toString();
    }

    /**
     * 一次性密码重置接口 - 重置admin密码为admin123
     * TODO: 密码验证通过后立即删除此接口！！！
     */
    @GetMapping("/debug/reset-password")
    public String resetAdminPassword() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Password Reset Result ===\n");

        try {
            Optional<User> userOpt = userRepository.findByUsername("admin");
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // 使用真实的PasswordEncoder生成新密码
                org.springframework.security.crypto.password.PasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

                String rawPassword = "admin123";
                String newEncodedPassword = encoder.encode(rawPassword);

                // 更新密码
                user.setPassword(newEncodedPassword);
                userRepository.save(user);

                sb.append("SUCCESS!\n");
                sb.append("New password hash (first 30 chars): ").append(newEncodedPassword.substring(0, 30)).append("...\n");
                sb.append("New password length: ").append(newEncodedPassword.length()).append("\n");
                sb.append("\nPlease login with:\n");
                sb.append("  Username: admin\n");
                sb.append("  Password: ").append(rawPassword).append("\n");
                sb.append("\n*** IMPORTANT: Delete this endpoint after verification! ***\n");
            } else {
                sb.append("ERROR: Admin user not found!");
            }
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        }

        return sb.toString();
    }
}
