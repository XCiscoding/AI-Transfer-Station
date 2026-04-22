package com.aikey.repository;

import com.aikey.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

/**
 * 调用日志数据访问层接口
 */
public interface CallLogRepository extends JpaRepository<CallLog, Long>, JpaSpecificationExecutor<CallLog> {

    long countByCreatedAtAfter(LocalDateTime since);

    long countByChannelIdAndCreatedAtAfter(Long channelId, LocalDateTime since);

    long countByStatusAndCreatedAtAfter(Integer status, LocalDateTime since);

    long countByChannelIdAndStatusAndCreatedAtAfter(Long channelId, Integer status, LocalDateTime since);
}
