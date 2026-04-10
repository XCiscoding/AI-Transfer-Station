package com.aikey.config;

import com.aikey.entity.User;
import com.aikey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    public static final List<String> BOOTSTRAP_SUPER_ADMIN_USERNAMES = List.of("enterprise_admin");

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            initBootstrapSuperAdminRoles();
        } catch (Exception e) {
            log.error("数据初始化失败", e);
        }
    }

    public void initBootstrapSuperAdminRoles() {
        log.info("========== DataInitializer: 开始检查引导超级管理员用户数据完整性 ==========");
        BOOTSTRAP_SUPER_ADMIN_USERNAMES.forEach(this::ensureBootstrapSuperAdminRole);
    }

    private void ensureBootstrapSuperAdminRole(String username) {
        User user = userRepository.findWithRolesByUsername(username).orElse(null);

        if (user == null) {
            log.warn("引导超级管理员用户 {} 不存在，跳过角色初始化", username);
            return;
        }

        int roleCount = user.getRoles() != null ? user.getRoles().size() : 0;
        log.info("用户 {} (userId={}) 当前角色数量: {}", user.getUsername(), user.getId(), roleCount);

        if (roleCount > 0) {
            log.info("引导超级管理员用户 {} 已有{}个角色关联，无需修复", user.getUsername(), roleCount);
            return;
        }

        log.warn("引导超级管理员用户 {} 没有关联任何角色，开始自动修复...", user.getUsername());
        log.warn("用户ID: {}, 邮箱: {}", user.getId(), user.getEmail());

        transactionTemplate.execute(status -> {
            try {
                Query selectRoleQuery = entityManager.createNativeQuery(
                        "SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN'");
                Object roleIdResult = selectRoleQuery.getSingleResult();

                if (roleIdResult == null) {
                    log.error("SUPER_ADMIN角色不存在，无法自动修复用户 {}", user.getUsername());
                    return null;
                }

                long roleId = ((Number) roleIdResult).longValue();
                log.info("找到SUPER_ADMIN角色，ID: {}", roleId);

                Query checkExistQuery = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?");
                checkExistQuery.setParameter(1, user.getId());
                checkExistQuery.setParameter(2, roleId);
                Number existCount = (Number) checkExistQuery.getSingleResult();

                if (existCount != null && existCount.intValue() > 0) {
                    log.info("用户角色关联已存在，无需重复插入 (userId={}, roleId={})", user.getId(), roleId);
                    return null;
                }

                Query insertQuery = entityManager.createNativeQuery(
                        "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)");
                insertQuery.setParameter(1, user.getId());
                insertQuery.setParameter(2, roleId);
                int rows = insertQuery.executeUpdate();

                if (rows > 0) {
                    log.info("成功为用户 {} 关联SUPER_ADMIN角色 (userId={}, roleId={})", user.getUsername(), user.getId(), roleId);
                } else {
                    log.error("插入user_roles失败，影响行数为0，用户 {}", user.getUsername());
                }
            } catch (Exception e) {
                log.error("自动修复用户 {} 角色关联失败", user.getUsername(), e);
                status.setRollbackOnly();
            }
            return null;
        });
    }
}
