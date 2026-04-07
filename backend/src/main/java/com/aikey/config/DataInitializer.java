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

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            initAdminUserRoles();
        } catch (Exception e) {
            log.error("数据初始化失败", e);
        }
    }

    public void initAdminUserRoles() {
        log.info("========== DataInitializer: 开始检查管理员用户数据完整性 ==========");

        User admin = userRepository.findWithRolesByUsername("admin").orElse(null);

        if (admin == null) {
            log.warn("管理员用户不存在，跳过角色初始化");
            return;
        }

        int roleCount = admin.getRoles() != null ? admin.getRoles().size() : 0;
        log.info("admin用户(userId={}) 当前角色数量: {}", admin.getId(), roleCount);

        if (roleCount > 0) {
            log.info("管理员用户 {} 已有{}个角色关联，无需修复", admin.getUsername(), roleCount);
            return;
        }

        log.warn("管理员用户 {} 没有关联任何角色！开始自动修复...", admin.getUsername());
        log.warn("用户ID: {}, 邮箱: {}", admin.getId(), admin.getEmail());

        transactionTemplate.execute(status -> {
            try {
                Query selectRoleQuery = entityManager.createNativeQuery(
                        "SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN'");
                Object roleIdResult = selectRoleQuery.getSingleResult();

                if (roleIdResult == null) {
                    log.error("SUPER_ADMIN角色不存在！无法自动修复");
                    return null;
                }

                long roleId = ((Number) roleIdResult).longValue();
                log.info("找到SUPER_ADMIN角色，ID: {}", roleId);

                Query checkExistQuery = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?");
                checkExistQuery.setParameter(1, admin.getId());
                checkExistQuery.setParameter(2, roleId);
                Number existCount = (Number) checkExistQuery.getSingleResult();

                if (existCount != null && existCount.intValue() > 0) {
                    log.info("用户角色关联已存在，无需重复插入 (userId={}, roleId={})", admin.getId(), roleId);
                    return null;
                }

                Query insertQuery = entityManager.createNativeQuery(
                        "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)");
                insertQuery.setParameter(1, admin.getId());
                insertQuery.setParameter(2, roleId);
                int rows = insertQuery.executeUpdate();

                if (rows > 0) {
                    log.info("成功为admin用户关联SUPER_ADMIN角色 (userId={}, roleId={})", admin.getId(), roleId);
                    log.info("========== 数据修复成功！admin用户现在可以正常登录 ==========");
                } else {
                    log.error("插入user_roles失败，影响行数为0");
                }
            } catch (Exception e) {
                log.error("自动修复角色关联失败", e);
                status.setRollbackOnly();
            }
            return null;
        });
    }
}
