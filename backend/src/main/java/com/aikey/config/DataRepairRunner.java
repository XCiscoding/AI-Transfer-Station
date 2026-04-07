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
 * 确保admin用户有正确的角色关联和密码，解决登录401问题</p>
 *
 * <p>使用原生JDBC而非JPA，避免循环依赖或事务问题</p>
 * <p>使用@Order确保在DataInitializer之后执行</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)  // 确保在DataInitializer（默认Order=0）之后执行
public class DataRepairRunner implements CommandLineRunner {

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("********************************************************");
        log.info("*        DataRepairRunner 开始执行（Order=2）          *");
        log.info("********************************************************");

        try {
            log.info("========== 开始数据完整性检查 ==========");

            // 检查并修复admin用户密码
            log.info("[Step 1] 检查并修复admin用户密码...");
            repairAdminUserPassword();

            // 检查并修复admin用户角色关联
            log.info("[Step 2] 检查并修复admin用户角色关联...");
            repairAdminUserRole();

            log.info("========== 数据完整性检查完成 ==========");
        } catch (Exception e) {
            log.error("========== 数据完整性检查失败 ==========", e);
            // 不阻止应用启动，只记录错误
            e.printStackTrace();
        }

        log.info("********************************************************");
        log.info("*        DataRepairRunner 执行完成                    *");
        log.info("********************************************************");
    }

    /**
     * 修复admin用户的密码
     *
     * <p>确保admin用户的密码是 admin123 的正确BCrypt哈希</p>
     */
    private void repairAdminUserPassword() throws Exception {
        log.info("--> 进入repairAdminUserPassword方法");
        try (Connection conn = dataSource.getConnection()) {

            // Step 1: 查询admin用户ID和当前密码哈希
            long userId;
            String currentPasswordHash;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, password FROM users WHERE username = ? AND deleted = 0")) {
                ps.setString(1, "admin");
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    log.warn("admin用户不存在，跳过密码修复");
                    return;
                }
                userId = rs.getLong("id");
                currentPasswordHash = rs.getString("password");
                log.info("找到admin用户，ID: {}, 当前密码哈希前20字符: {}...",
                        userId, currentPasswordHash.substring(0, Math.min(20, currentPasswordHash.length())));
            }

            // Step 2: 验证密码是否正确（admin123）
            String correctPassword = "admin123";
            boolean passwordValid = passwordEncoder.matches(correctPassword, currentPasswordHash);
            log.info("密码验证结果: {}", passwordValid ? "通过" : "不通过");

            if (passwordValid) {
                log.info("✅ admin用户密码验证通过");
                return;
            }

            log.warn("⚠️  admin用户密码不正确，开始自动修复...");

            // Step 3: 生成正确的密码哈希并更新
            String newPasswordHash = passwordEncoder.encode(correctPassword);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET password = ? WHERE id = ?")) {
                ps.setString(1, newPasswordHash);
                ps.setLong(2, userId);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    log.info("✅ 成功修复admin用户密码 (userId={})", userId);
                    log.info("   新密码哈希: {}", newPasswordHash);
                } else {
                    log.error("❌ 更新admin密码失败，影响行数为0");
                }
            }
        }
    }

    /**
     * 修复admin用户的角色关联
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>查询admin用户是否存在</li>
     *   <li>检查是否已有角色关联</li>
     *   <li>如果没有则自动关联SUPER_ADMIN角色</li>
     * </ol>
     */
    private void repairAdminUserRole() throws Exception {
        log.info("--> 进入repairAdminUserRole方法");
        try (Connection conn = dataSource.getConnection()) {

            // Step 1: 查询admin用户ID
            long userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM users WHERE username = ? AND deleted = 0")) {
                ps.setString(1, "admin");
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    log.warn("admin用户不存在，跳过角色修复");
                    return;
                }
                userId = rs.getLong("id");
                log.info("找到admin用户，ID: {}", userId);
            }

            // Step 2: 查询是否已有角色关联
            int count;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM user_roles WHERE user_id = ?")) {
                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();
                rs.next();
                count = rs.getInt(1);
            }

            log.info("admin用户(userId={}) 当前角色关联数量: {}", userId, count);

            if (count > 0) {
                log.info("✅ admin用户(userId={}) 已有{}个角色关联，无需修复", userId, count);
                return;
            }

            log.warn("⚠️  admin用户(userId={}) 没有角色关联，开始自动修复...", userId);

            // Step 3: 查询SUPER_ADMIN角色ID
            long roleId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM roles WHERE role_code = ?")) {
                ps.setString(1, "SUPER_ADMIN");
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    log.error("❌ SUPER_ADMIN角色不存在！无法自动修复");
                    return;
                }
                roleId = rs.getLong("id");
                log.info("找到SUPER_ADMIN角色，ID: {}", roleId);
            }

            // Step 4: 插入角色关联
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                ps.setLong(1, userId);
                ps.setLong(2, roleId);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    log.info("✅ 成功为admin用户关联SUPER_ADMIN角色 (userId={}, roleId={})", userId, roleId);
                    log.info("========== 数据修复成功！admin用户现在可以正常登录 ==========");
                } else {
                    log.error("❌ 插入user_roles失败，影响行数为0");
                }
            }
        }
    }
}
