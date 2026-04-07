package com.aikey.repository;

import com.aikey.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层接口
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户Optional对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据用户名查询用户（同时加载角色关联）
     *
     * @param username 用户名
     * @return 用户Optional对象（包含roles集合）
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findWithRolesByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户Optional对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 判断用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 判断邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据状态和删除标记查询用户列表
     *
     * @param status 状态（0-禁用，1-启用）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 用户列表
     */
    List<User> findByStatusAndDeleted(Integer status, Integer deleted);
}
