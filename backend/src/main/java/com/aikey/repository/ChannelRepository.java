package com.aikey.repository;

import com.aikey.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 渠道数据访问层接口
 */
public interface ChannelRepository extends JpaRepository<Channel, Long>, JpaSpecificationExecutor<Channel> {

    /**
     * 根据状态和删除标记查询渠道列表（按优先级降序）
     *
     * @param status 状态（0-禁用，1-启用，2-维护中）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 渠道列表
     */
    List<Channel> findByStatusAndDeletedOrderByPriorityDesc(Integer status, Integer deleted);

    /**
     * 根据渠道类型、状态和删除标记查询渠道列表
     *
     * @param channelType 渠道类型（openai/qwen/wenxin/doubao/claude/gemini/deepseek）
     * @param status 状态（0-禁用，1-启用，2-维护中）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 渠道列表
     */
    List<Channel> findByChannelTypeAndStatusAndDeleted(String channelType, Integer status, Integer deleted);

    /**
     * 统计指定状态和删除标记的渠道数量
     *
     * @param status 状态（0-禁用，1-启用，2-维护中）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 渠道数量
     */
    long countByStatusAndDeleted(Integer status, Integer deleted);

    /**
     * 根据渠道编码查询渠道
     *
     * @param channelCode 渠道编码
     * @return 渠道Optional对象
     */
    Optional<Channel> findByChannelCode(String channelCode);
}
