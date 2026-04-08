package com.aikey.service;

import com.aikey.dto.calllog.CallLogQueryRequest;
import com.aikey.dto.calllog.CallLogVO;
import com.aikey.dto.common.PageResult;
import com.aikey.entity.CallLog;
import com.aikey.repository.CallLogRepository;
import com.aikey.repository.ChannelRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调用日志查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallLogQueryService {

    private final CallLogRepository callLogRepository;
    private final ChannelRepository channelRepository;

    /**
     * 动态条件分页查询调用日志
     */
    public PageResult<CallLogVO> queryLogs(CallLogQueryRequest req) {
        Specification<CallLog> spec = buildSpec(req);

        Page<CallLog> pageResult = callLogRepository.findAll(
                spec,
                PageRequest.of(req.getPage() - 1, req.getSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<CallLogVO> records = pageResult.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.<CallLogVO>builder()
                .records(records)
                .total(pageResult.getTotalElements())
                .current((long) req.getPage())
                .size((long) req.getSize())
                .build();
    }

    private Specification<CallLog> buildSpec(CallLogQueryRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), req.getStartTime()));
            }
            if (req.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), req.getEndTime()));
            }
            if (StringUtils.hasText(req.getModelName())) {
                String pattern = "%" + req.getModelName() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("modelName"), pattern),
                        cb.like(root.get("requestModel"), pattern)
                ));
            }
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            if (req.getChannelId() != null) {
                predicates.add(cb.equal(root.get("channelId"), req.getChannelId()));
            }
            if (req.getVirtualKeyId() != null) {
                predicates.add(cb.equal(root.get("virtualKeyId"), req.getVirtualKeyId()));
            }
            if (req.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), req.getUserId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CallLogVO convertToVO(CallLog log) {
        // 补充渠道名称（简单 findById，不存在则 null）
        String channelName = null;
        if (log.getChannelId() != null) {
            channelName = channelRepository.findById(log.getChannelId())
                    .map(c -> c.getChannelName())
                    .orElse(null);
        }

        return CallLogVO.builder()
                .id(log.getId())
                .traceId(log.getTraceId())
                .userId(log.getUserId())
                .virtualKeyId(log.getVirtualKeyId())
                .channelId(log.getChannelId())
                .modelId(log.getModelId())
                .modelName(log.getModelName())
                .requestModel(log.getRequestModel())
                .promptTokens(log.getPromptTokens())
                .completionTokens(log.getCompletionTokens())
                .totalTokens(log.getTotalTokens())
                .weightedAmount(null)   // 后期关联流水表填充
                .responseTime(log.getResponseTime())
                .status(log.getStatus())
                .errorCode(log.getErrorCode())
                .errorMessage(log.getErrorMessage())
                .clientIp(log.getClientIp())
                .createdAt(log.getCreatedAt())
                .channelName(channelName)
                .build();
    }
}
