package com.aikey.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 数据修复运行器
 *
 * <p>应用启动完成后自动执行数据完整性检查和自动修复，
 * 确保引导超级管理员用户有正确的角色关联和密码，解决登录401问题</p>
 *
 * <p>使用原生JDBC而非JPA，避免循环依赖或事务问题</p>
 * <p>使用@Order确保在DataInitializer之后执行</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class DataRepairRunner implements CommandLineRunner {

    private static final String DEFAULT_BOOTSTRAP_PASSWORD = "admin123";
    private static final String BOOTSTRAP_SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("********************************************************");
        log.info("*        DataRepairRunner 开始执行（Order=2）          *");
        log.info("********************************************************");

        try {
            log.info("========== 开始数据完整性检查 ==========");

            for (String username : DataInitializer.BOOTSTRAP_SUPER_ADMIN_USERNAMES) {
                log.info("[Bootstrap User] 开始检查用户: {}", username);
                repairBootstrapUserPassword(username);
                repairBootstrapUserRole(username);
            }

            log.info("========== 数据完整性检查完成 ==========");
        } catch (Exception e) {
            log.error("========== 数据完整性检查失败 ========= =", e);
            e.printStackTrace();
        }

        log.info("********************************************************");
        log.info("*        DataRepairRunner 执行完成                    *");
        log.info("********************************************************");
    }

    private void repairBootstrapUserPassword(String username) throws Exception {
        log.info("--> 检查用户 {} 的密码", username);
        try (Connection conn = dataSource.getConnection()) {
            long userId;
            String currentPasswordHash;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, password FROM users WHERE username = ? AND deleted = 0")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    log.warn("用户 {} 不存在，跳过密码修复", username);
                    return;
                }
                userId = rs.getLong("id");
                currentPasswordHash = rs.getString("password");
                log.info("找到用户 {}，ID: {}, 当前密码哈希前20字符: {}...",
                        username, userId, currentPasswordHash.substring(0, Math.min(20, currentPasswordHash.length())));
            }

            boolean passwordValid = passwordEncoder.matches(DEFAULT_BOOTSTRAP_PASSWORD, currentPasswordHash);
            log.info("用户 {} 密码验证结果: {}", username, passwordValid ? "通过" : "不通过");

            if (passwordValid) {
                log.info("✅ 用户 {} 密码验证通过", username);
                return;
            }

            log.warn("⚠️ 用户 {} 密码不正确，开始自动修复...", username);

            String newPasswordHash = passwordEncoder.encode(DEFAULT_BOOTSTRAP_PASSWORD);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET password = ? WHERE id = ?")) {
                ps.setString(1, newPasswordHash);
                ps.setLong(2, userId);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    log.info("✅ 成功修复用户 {} 密码 (userId={})", username, userId);
                } else {
                    log.error("❌ 更新用户 {} 密码失败，影响行数为0", username);
                }
            }
        }
    }

    private void repairBootstrapUserRole(String username) throws Exception {
        log.info("--> 检查用户 {} 的角色关联", username);
        try (Connection conn = dataSource.getConnection()) {
            long userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ? AND deleted = 0")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    log.warn("用户 {} 不存在，跳过角色修复", username);
                    return;
                }
                userId = rs.getLong("id");
                log.info("找到用户 {}，ID: {}", username, userId);
            }

            int count;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM user_roles WHERE user_id = ?")) {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();
                rs.next();
                count = rs.getInt(1);
            }

            log.info("用户 {} (userId={}) 当前角色关联数量: {}", username, userId, count);

            if (count > 0) {
                log.info("✅ 用户 {} (userId={}) 已有{}个角色关联，无需修复", username, userId, count);
                return;
            }

            log.warn("⚠️ 用户 {} (userId={}) 没有角色关联，开始自动修复...", username, userId);

            long roleId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM roles WHERE role_code = ?")) {
                ps.setString(1, BOOTSTRAP_SUPER_ADMIN_ROLE);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    log.error("❌ {}角色不存在，无法自动修复用户 {}", BOOTSTRAP_SUPER_ADMIN_ROLE, username);
                    return;
                }
                roleId = rs.getLong("id");
                log.info("找到{}角色，ID: {}", BOOTSTRAP_SUPER_ADMIN_ROLE, roleId);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                ps.setLong(1, userId);
                ps.setLong(2, roleId);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    log.info("✅ 成功为用户 {} 关联{}角色 (userId={}, roleId={})", username, BOOTSTRAP_SUPER_ADMIN_ROLE, userId, roleId);
                } else {
                    log.error("❌ 插入用户 {} 的user_roles失败，影响行数为0", username);
                }
            }
        }
    }
}
