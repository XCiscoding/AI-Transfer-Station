package com.aikey.repository;

import com.aikey.entity.RealKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 真实API Key数据访问层接口
 */
public interface RealKeyRepository extends JpaRepository<RealKey, Long>, JpaSpecificationExecutor<RealKey> {

    /**
     * 根据渠道ID查询所有有效的真实Key（按创建时间降序）
     *
     * @param channelId 渠道ID
     * @param deleted   删除标记（0-未删除，1-已删除）
     * @return 真实Key列表
     */
    List<RealKey> findByChannelIdAndDeletedOrderByCreatedAtDesc(Long channelId, Integer deleted);

    /**
     * 根据状态和删除标记查询真实Key列表
     *
     * @param status 状态（0-禁用，1-启用）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 真实Key列表
     */
    List<RealKey> findByStatusAndDeleted(Integer status, Integer deleted);

    /**
     * 统计指定渠道某状态的真实Key数量
     *
     * @param channelId 渠道ID
     * @param status    状态（0-禁用，1-启用）
     * @param deleted   删除标记（0-未删除，1-已删除）
     * @return Key数量
     */
    long countByChannelIdAndStatusAndDeleted(Long channelId, Integer status, Integer deleted);

    /**
     * 根据ID查询真实Key（排除已删除记录）
     *
     * @param id      Key ID
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 真实Key的Optional对象
     */
    Optional<RealKey> findByIdAndDeleted(Long id, Integer deleted);
}
