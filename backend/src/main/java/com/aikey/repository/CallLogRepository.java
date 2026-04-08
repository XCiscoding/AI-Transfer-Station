package com.aikey.repository;

import com.aikey.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 调用日志数据访问层接口
 */
public interface CallLogRepository extends JpaRepository<CallLog, Long>, JpaSpecificationExecutor<CallLog> {
}
