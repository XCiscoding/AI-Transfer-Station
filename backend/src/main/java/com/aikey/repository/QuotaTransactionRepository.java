package com.aikey.repository;

import com.aikey.entity.QuotaTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 额度流水数据访问层
 */
public interface QuotaTransactionRepository extends JpaRepository<QuotaTransaction, Long> {

    /**
     * 按目标对象分页查询流水
     */
    Page<QuotaTransaction> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            String targetType, Long targetId, Pageable pageable);

    /**
     * 按虚拟Key分页查询流水
     */
    Page<QuotaTransaction> findByVirtualKeyIdOrderByCreatedAtDesc(Long virtualKeyId, Pageable pageable);

    /**
     * 按用户ID分页查询流水
     */
    Page<QuotaTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查询某对象在时间范围内的消耗总量
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM QuotaTransaction t " +
           "WHERE t.targetType = :targetType AND t.targetId = :targetId " +
           "AND t.transactionType = 'consume' " +
           "AND t.createdAt >= :from AND t.createdAt < :to")
    java.math.BigDecimal sumConsumedBetween(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
