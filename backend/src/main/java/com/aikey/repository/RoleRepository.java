package com.aikey.repository;

import com.aikey.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问层接口
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色Optional对象
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 根据状态和删除标记查询角色列表
     *
     * @param status 状态（0-禁用，1-启用）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 角色列表
     */
    List<Role> findByStatusAndDeleted(Integer status, Integer deleted);
}
