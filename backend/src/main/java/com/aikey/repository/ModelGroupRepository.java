package com.aikey.repository;

import com.aikey.entity.ModelGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 模型分组数据访问层
 */
public interface ModelGroupRepository extends JpaRepository<ModelGroup, Long>, JpaSpecificationExecutor<ModelGroup> {

    /**
     * 判断同名分组（未删除）是否存在
     */
    boolean existsByGroupNameAndDeleted(String groupName, Integer deleted);

    /**
     * 按ID和删除标记查询分组
     */
    Optional<ModelGroup> findByIdAndDeleted(Long id, Integer deleted);
}
