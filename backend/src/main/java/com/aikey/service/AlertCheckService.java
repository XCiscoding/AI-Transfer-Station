package com.aikey.service;

import com.aikey.entity.AlertHistory;
import com.aikey.entity.AlertRule;
import com.aikey.entity.Channel;
import com.aikey.entity.VirtualKey;
import com.aikey.repository.AlertHistoryRepository;
import com.aikey.repository.AlertRuleRepository;
import com.aikey.repository.CallLogRepository;
import com.aikey.repository.ChannelRepository;
import com.aikey.repository.VirtualKeyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警触发引擎
 * 每分钟检查所有已启用告警规则，满足条件时写入告警历史（1小时去重）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCheckService {

    private final AlertRuleService alertRuleService;
    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final VirtualKeyRepository virtualKeyRepository;
    private final ChannelRepository channelRepository;
    private final CallLogRepository callLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedRate = 60000)
    public void checkAlertRules() {
        List<AlertRule> rules = alertRuleService.listEnabledRules();
        log.debug("检查告警规则，共 {} 条", rules.size());
        for (AlertRule rule : rules) {
            try {
                checkRule(rule);
            } catch (Exception e) {
                log.error("检测告警规则 id={} name={} 失败: {}", rule.getId(), rule.getRuleName(), e.getMessage());
            }
        }
    }

    private void checkRule(AlertRule rule) throws Exception {
        boolean triggered = evaluateRule(rule);
        if (!triggered) return;

        // 1小时去重：同一规则在1小时内不重复触发
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        if (alertHistoryRepository.existsByRuleIdAndCreatedAtAfter(rule.getId(), oneHourAgo)) {
            log.debug("规则 id={} 1小时内已触发过，跳过", rule.getId());
            return;
        }

        // 写入告警历史
        AlertHistory history = buildAlertHistory(rule);
        alertHistoryRepository.save(history);

        // 更新规则统计
        rule.setLastTriggeredTime(LocalDateTime.now());
        rule.setTriggerCount(rule.getTriggerCount() + 1);
        alertRuleRepository.save(rule);

        log.info("告警触发: id={} name={} type={}", rule.getId(), rule.getRuleName(), rule.getRuleType());
    }

    private boolean evaluateRule(AlertRule rule) throws Exception {
        return switch (rule.getRuleType()) {
            case "quota_low" -> checkQuotaLow(rule);
            case "channel_down" -> checkChannelDown(rule);
            case "call_volume" -> checkCallVolume(rule);
            case "error_rate" -> checkErrorRate(rule);
            default -> {
                log.warn("未知的告警规则类型: {}", rule.getRuleType());
                yield false;
            }
        };
    }

    /**
     * quota_low: 虚拟Key剩余额度占比 < threshold%
     * conditionConfig: {"threshold": 20}
     */
    private boolean checkQuotaLow(AlertRule rule) throws Exception {
        Long targetId = rule.getTargetId();
        if (targetId == null || targetId <= 0) return false;

        Optional<VirtualKey> opt = virtualKeyRepository.findById(targetId);
        if (opt.isEmpty()) return false;

        VirtualKey vk = opt.get();
        if (Integer.valueOf(0).equals(vk.getStatus())) return false;

        BigDecimal limit = vk.getQuotaLimit();
        BigDecimal remaining = vk.getQuotaRemaining();
        if (limit == null || limit.compareTo(BigDecimal.ZERO) == 0) return false;

        JsonNode config = parseConfig(rule.getConditionConfig());
        double threshold = config.path("threshold").asDouble(20.0);

        double remainingPercent = remaining
                .divide(limit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        return remainingPercent < threshold;
    }

    /**
     * channel_down: 渠道健康状态异常 或 失败次数 >= failThreshold
     * conditionConfig: {"failThreshold": 5}
     */
    private boolean checkChannelDown(AlertRule rule) throws Exception {
        Long targetId = rule.getTargetId();
        if (targetId == null || targetId <= 0) return false;

        Optional<Channel> opt = channelRepository.findById(targetId);
        if (opt.isEmpty()) return false;

        Channel channel = opt.get();
        JsonNode config = parseConfig(rule.getConditionConfig());
        long failThreshold = config.path("failThreshold").asLong(5);

        return Integer.valueOf(0).equals(channel.getHealthStatus())
                || (channel.getFailCount() != null && channel.getFailCount() >= failThreshold);
    }

    /**
     * call_volume: 最近1分钟调用量 > maxCount
     * conditionConfig: {"maxCount": 100}
     * targetId 为渠道ID（0/null 表示全局）
     */
    private boolean checkCallVolume(AlertRule rule) throws Exception {
        JsonNode config = parseConfig(rule.getConditionConfig());
        long maxCount = config.path("maxCount").asLong(100);

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        Long targetId = rule.getTargetId();

        long count = (targetId != null && targetId > 0)
                ? callLogRepository.countByChannelIdAndCreatedAtAfter(targetId, oneMinuteAgo)
                : callLogRepository.countByCreatedAtAfter(oneMinuteAgo);

        return count > maxCount;
    }

    /**
     * error_rate: 最近5分钟错误率 > maxRate
     * conditionConfig: {"maxRate": 0.5}  (0.5 = 50%)
     * targetId 为渠道ID（0/null 表示全局）
     */
    private boolean checkErrorRate(AlertRule rule) throws Exception {
        JsonNode config = parseConfig(rule.getConditionConfig());
        double maxRate = config.path("maxRate").asDouble(0.5);

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        Long targetId = rule.getTargetId();

        long total;
        long errors;
        if (targetId != null && targetId > 0) {
            total = callLogRepository.countByChannelIdAndCreatedAtAfter(targetId, fiveMinutesAgo);
            errors = callLogRepository.countByChannelIdAndStatusAndCreatedAtAfter(targetId, 0, fiveMinutesAgo);
        } else {
            total = callLogRepository.countByCreatedAtAfter(fiveMinutesAgo);
            errors = callLogRepository.countByStatusAndCreatedAtAfter(0, fiveMinutesAgo);
        }

        if (total == 0) return false;
        return (double) errors / total > maxRate;
    }

    private JsonNode parseConfig(String configJson) throws Exception {
        if (configJson == null || configJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(configJson);
    }

    private AlertHistory buildAlertHistory(AlertRule rule) {
        return AlertHistory.builder()
                .ruleId(rule.getId())
                .alertType(rule.getRuleType())
                .alertLevel(resolveAlertLevel(rule.getRuleType()))
                .alertTitle(buildAlertTitle(rule))
                .alertContent(buildAlertContent(rule))
                .targetType(rule.getTargetType())
                .targetId(rule.getTargetId())
                .notificationStatus("pending")
                .build();
    }

    private String buildAlertTitle(AlertRule rule) {
        return switch (rule.getRuleType()) {
            case "quota_low" -> "额度告警：" + rule.getRuleName();
            case "channel_down" -> "渠道异常：" + rule.getRuleName();
            case "call_volume" -> "流量告警：" + rule.getRuleName();
            case "error_rate" -> "错误率告警：" + rule.getRuleName();
            default -> "系统告警：" + rule.getRuleName();
        };
    }

    private String buildAlertContent(AlertRule rule) {
        return String.format("规则「%s」(类型:%s) 已满足触发条件。检测配置: %s",
                rule.getRuleName(), rule.getRuleType(), rule.getConditionConfig());
    }

    private String resolveAlertLevel(String ruleType) {
        return switch (ruleType) {
            case "channel_down" -> "critical";
            case "error_rate", "quota_low" -> "warning";
            default -> "info";
        };
    }
}
