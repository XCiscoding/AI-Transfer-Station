package com.aikey.repository;

import com.aikey.entity.VirtualKey;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 虚拟API Key数据访问层接口
 *
 * <p>提供虚拟Key的CRUD操作、按用户/状态查询、分页查询等功能。
 * 支持JpaSpecificationExecutor用于后续动态查询场景。</p>
 */
public interface VirtualKeyRepository extends JpaRepository<VirtualKey, Long>, JpaSpecificationExecutor<VirtualKey> {

    /**
     * 根据Key值查询虚拟Key
     *
     * @param keyValue Key值（sk-xxx格式）
     * @return 虚拟Key的Optional对象
     */
    Optional<VirtualKey> findByKeyValue(String keyValue);

    /**
     * 根据用户ID和删除标记查询虚拟Key列表
     *
     * @param userId  用户ID
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 虚拟Key列表
     */
    List<VirtualKey> findByUserIdAndDeleted(Long userId, Integer deleted);

    /**
     * 根据状态和删除标记查询虚拟Key列表
     *
     * @param status 状态（0-禁用，1-启用）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 虚拟Key列表
     */
    List<VirtualKey> findByStatusAndDeleted(Integer status, Integer deleted);

    List<VirtualKey> findByTeamIdInAndDeleted(List<Long> teamIds, Integer deleted);

    /**
     * 根据用户ID和删除标记分页查询虚拟Key
     *
     * @param userId   用户ID
     * @param deleted  删除标记（0-未删除，1-已删除）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<VirtualKey> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);

    /**
     * 判断Key值是否已存在
     *
     * @param keyValue Key值（sk-xxx格式）
     * @return 是否存在
     */
    boolean existsByKeyValue(String keyValue);

    /**
     * 仅更新最后使用时间，避免覆盖额度字段的并发/原子扣减结果。
     */
    @Transactional
    @Modifying
    @Query("UPDATE VirtualKey v SET v.lastUsedTime = :lastUsedTime WHERE v.id = :id")
    int updateLastUsedTime(@Param("id") Long id, @Param("lastUsedTime") LocalDateTime lastUsedTime);
}
