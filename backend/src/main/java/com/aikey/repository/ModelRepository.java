package com.aikey.repository;

import com.aikey.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 模型数据访问层接口
 */
public interface ModelRepository extends JpaRepository<Model, Long>, JpaSpecificationExecutor<Model> {

    /**
     * 根据渠道ID和删除标记查询模型列表
     *
     * @param channelId 渠道ID
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 模型列表
     */
    List<Model> findByChannelIdAndDeleted(Long channelId, Integer deleted);

    /**
     * 根据模型编码、渠道ID和删除标记查询模型
     *
     * @param modelCode 模型编码
     * @param channelId 渠道ID
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 模型Optional对象
     */
    List<Model> findByModelCodeAndChannelIdAndDeleted(String modelCode, Long channelId, Integer deleted);

    /**
     * 根据模型类型、状态和删除标记查询模型列表
     *
     * @param modelType 模型类型（chat/embedding/image）
     * @param status 状态（0-下线，1-上线）
     * @param deleted 删除标记（0-未删除，1-已删除）
     * @return 模型列表
     */
    List<Model> findByModelTypeAndStatusAndDeleted(String modelType, Integer status, Integer deleted);

    /**
     * 根据模型编码、状态和删除标记查询所有匹配的模型（跨渠道）
     * 用于调度引擎筛选候选渠道
     *
     * @param modelCode 模型编码（如gpt-4、deepseek-chat）
     * @param status    状态（0-下线，1-上线）
     * @param deleted   删除标记（0-未删除，1-已删除）
     * @return 所有匹配的模型列表
     */
    List<Model> findByModelCodeAndStatusAndDeleted(String modelCode, Integer status, Integer deleted);
}
