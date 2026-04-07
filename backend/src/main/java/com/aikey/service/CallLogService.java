package com.aikey.service;

import com.aikey.entity.CallLog;
import com.aikey.repository.CallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 调用日志服务
 *
 * <p>提供异步写入调用日志的功能，不阻塞主请求流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallLogService {

    private final CallLogRepository callLogRepository;

    /**
     * 异步记录调用日志
     *
     * @param callLog 调用日志实体
     */
    @Async
    public void recordAsync(CallLog callLog) {
        try {
            callLogRepository.save(callLog);
            log.debug("调用日志记录成功: traceId={}", callLog.getTraceId());
        } catch (Exception e) {
            log.error("调用日志记录失败: traceId={}, error={}", callLog.getTraceId(), e.getMessage(), e);
        }
    }
}
