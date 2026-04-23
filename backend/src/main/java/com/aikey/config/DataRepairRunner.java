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
import java.sql.Statement;

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
    private static final String DEFAULT_USER_ROLE = "USER";
    private static final String TEAM_ADMIN_USERNAME = "team_admin";
    private static final String TEAM_ADMIN_PASSWORD = "team123456";
    private static final String DEMO_USER_USERNAME = "demo_user";
    private static final String DEMO_USER_PASSWORD = "user123456";

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
            ensureDemoAccounts();

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

    private void ensureDemoAccounts() throws Exception {
        log.info("========== 开始检查演示账号 ==========");
        try (Connection conn = dataSource.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                long teamAdminId = ensureUser(conn, TEAM_ADMIN_USERNAME, TEAM_ADMIN_PASSWORD,
                        "team_admin@example.com", "团队管理员演示账号");
                long demoUserId = ensureUser(conn, DEMO_USER_USERNAME, DEMO_USER_PASSWORD,
                        "demo_user@example.com", "普通用户演示账号");

                ensureUserRole(conn, teamAdminId, DEFAULT_USER_ROLE);
                ensureUserRole(conn, demoUserId, DEFAULT_USER_ROLE);

                Long defaultTeamId = queryLong(conn, "SELECT id FROM teams WHERE team_code = 'default-team' AND deleted = 0 LIMIT 1");
                if (defaultTeamId != null) {
                    updateDefaultTeamOwner(conn, defaultTeamId, teamAdminId);
                    ensureTeamMember(conn, defaultTeamId, teamAdminId, "owner");
                    ensureTeamMember(conn, defaultTeamId, demoUserId, "member");
                    normalizeTeamOwner(conn, defaultTeamId, teamAdminId);
                    updateDefaultProjectOwner(conn, defaultTeamId, teamAdminId);
                    ensureDemoVirtualKey(conn, defaultTeamId, demoUserId);
                } else {
                    log.warn("默认团队不存在，跳过演示账号团队绑定");
                }

                ensureUserQuota(conn, teamAdminId);
                ensureUserQuota(conn, demoUserId);

                conn.commit();
                conn.setAutoCommit(oldAutoCommit);
                log.info("✅ 演示账号检查完成: {} / {}, {} / {}",
                        TEAM_ADMIN_USERNAME, TEAM_ADMIN_PASSWORD, DEMO_USER_USERNAME, DEMO_USER_PASSWORD);
            } catch (Exception e) {
                conn.rollback();
                conn.setAutoCommit(oldAutoCommit);
                throw e;
            }
        }
    }

    private long ensureUser(Connection conn, String username, String rawPassword, String email, String realName) throws Exception {
        Long userId = null;
        String currentPasswordHash = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, password FROM users WHERE username = ? AND deleted = 0")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getLong("id");
                currentPasswordHash = rs.getString("password");
            }
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        if (userId == null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, email, real_name, status, is_locked, login_fail_count, deleted) VALUES (?, ?, ?, ?, 1, 0, 0, 0)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.setString(3, email);
                ps.setString(4, realName);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            Long insertedId = queryLong(conn, "SELECT id FROM users WHERE username = '" + username + "' AND deleted = 0 LIMIT 1");
            if (insertedId == null) {
                throw new IllegalStateException("演示用户创建后无法查询: " + username);
            }
            return insertedId;
        }

        boolean passwordValid = currentPasswordHash != null && passwordEncoder.matches(rawPassword, currentPasswordHash);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET password = ?, email = ?, real_name = ?, status = 1, is_locked = 0 WHERE id = ?")) {
            ps.setString(1, passwordValid ? currentPasswordHash : passwordHash);
            ps.setString(2, email);
            ps.setString(3, realName);
            ps.setLong(4, userId);
            ps.executeUpdate();
        }
        return userId;
    }

    private void ensureUserRole(Connection conn, long userId, String roleCode) throws Exception {
        Long roleId = queryLong(conn, "SELECT id FROM roles WHERE role_code = '" + roleCode + "' LIMIT 1");
        if (roleId == null) {
            throw new IllegalStateException("角色不存在: " + roleCode);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
            ps.setLong(1, userId);
            ps.setLong(2, roleId);
            ps.executeUpdate();
        }
    }

    private void updateDefaultTeamOwner(Connection conn, long teamId, long ownerId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE teams SET owner_id = ?, updated_at = NOW() WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, ownerId);
            ps.setLong(2, teamId);
            ps.executeUpdate();
        }
    }

    private void ensureTeamMember(Connection conn, long teamId, long userId, String role) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM team_members WHERE team_id = ? AND user_id = ?")) {
            ps.setLong(1, teamId);
            ps.setLong(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE team_members SET role = ? WHERE id = ?")) {
                    update.setString(1, role);
                    update.setLong(2, rs.getLong("id"));
                    update.executeUpdate();
                }
                return;
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO team_members (team_id, user_id, role, joined_at) VALUES (?, ?, ?, NOW())")) {
            ps.setLong(1, teamId);
            ps.setLong(2, userId);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    private void normalizeTeamOwner(Connection conn, long teamId, long ownerId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE team_members SET role = 'member' WHERE team_id = ? AND user_id <> ? AND role = 'owner'")) {
            ps.setLong(1, teamId);
            ps.setLong(2, ownerId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE teams SET member_count = (SELECT COUNT(*) FROM team_members WHERE team_id = ?) WHERE id = ?")) {
            ps.setLong(1, teamId);
            ps.setLong(2, teamId);
            ps.executeUpdate();
        }
    }

    private void updateDefaultProjectOwner(Connection conn, long teamId, long ownerId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE projects SET owner_id = ?, updated_at = NOW() WHERE team_id = ? AND deleted = 0")) {
            ps.setLong(1, ownerId);
            ps.setLong(2, teamId);
            ps.executeUpdate();
        }
    }

    private void ensureUserQuota(Connection conn, long userId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO quotas (user_id, quota_type, quota_limit, quota_used, quota_remaining, reset_cycle) " +
                        "SELECT ?, 'token', 1000000.00, 0.00, 1000000.00, 'never' " +
                        "WHERE NOT EXISTS (SELECT 1 FROM quotas WHERE user_id = ? AND quota_type = 'token')")) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private void ensureDemoVirtualKey(Connection conn, long teamId, long demoUserId) throws Exception {
        Long projectId = queryLong(conn, "SELECT id FROM projects WHERE team_id = " + teamId + " AND deleted = 0 ORDER BY id LIMIT 1");
        Long channelId = queryLong(conn, "SELECT id FROM channels WHERE channel_code = 'zhipu' AND deleted = 0 ORDER BY id LIMIT 1");
        Long groupId = queryLong(conn, "SELECT id FROM model_groups WHERE group_code = 'china-llm' AND deleted = 0 ORDER BY id LIMIT 1");
        if (projectId == null || channelId == null || groupId == null) {
            log.warn("演示虚拟Key依赖数据不完整，跳过创建: projectId={}, channelId={}, groupId={}", projectId, channelId, groupId);
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO virtual_keys (key_name, key_value, user_id, team_id, project_id, allowed_models, allowed_group_ids, channel_id, " +
                        "quota_type, quota_limit, quota_used, quota_remaining, rate_limit_qpm, rate_limit_qpd, status, remark, deleted) " +
                        "SELECT '普通用户演示Key', 'sk-demo-user-low-completion-001', ?, ?, ?, NULL, ?, ?, " +
                        "'token', 100000.00, 0.00, 100000.00, 60, 0, 1, '供 demo_user 领取和复制的低完成度演示Key', 0 " +
                        "WHERE NOT EXISTS (SELECT 1 FROM virtual_keys WHERE key_value = 'sk-demo-user-low-completion-001')")) {
            ps.setLong(1, demoUserId);
            ps.setLong(2, teamId);
            ps.setLong(3, projectId);
            ps.setString(4, "[" + groupId + "]");
            ps.setLong(5, channelId);
            ps.executeUpdate();
        }
    }

    private Long queryLong(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return null;
        }
    }
}
