package com.aikey.repository;

import com.aikey.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 权限数据访问层接口
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据资源类型查询权限列表
     *
     * @param resourceType 资源类型（menu-菜单，button-按钮，api-接口）
     * @return 权限列表
     */
    List<Permission> findByResourceType(String resourceType);

    /**
     * 根据父权限ID查询权限列表
     *
     * @param parentId 父权限ID
     * @return 权限列表
     */
    List<Permission> findByParentId(Long parentId);
}
