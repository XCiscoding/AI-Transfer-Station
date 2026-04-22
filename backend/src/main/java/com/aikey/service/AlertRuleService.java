package com.aikey.service;

import com.aikey.dto.common.PageResult;
import com.aikey.entity.AlertHistory;
import com.aikey.entity.AlertRule;
import com.aikey.repository.AlertHistoryRepository;
import com.aikey.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    @Transactional(readOnly = true)
    public PageResult<AlertRule> listRules(int page, int size) {
        Page<AlertRule> pageResult = alertRuleRepository.findByDeletedOrderByCreatedAtDesc(
                0, PageRequest.of(page - 1, size));
        return PageResult.<AlertRule>builder()
                .records(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    public AlertRule createRule(String ruleName, String ruleType, String targetType, Long targetId,
                                String conditionConfig, String actionConfig,
                                String notificationChannels, Integer isEnabled) {
        AlertRule rule = AlertRule.builder()
                .ruleName(ruleName)
                .ruleType(ruleType)
                .targetType(targetType)
                .targetId(targetId)
                .conditionConfig(conditionConfig)
                .actionConfig(actionConfig)
                .notificationChannels(notificationChannels)
                .isEnabled(isEnabled != null ? isEnabled : 1)
                .build();
        return alertRuleRepository.save(rule);
    }

    public AlertRule updateRule(Long id, String ruleName, String ruleType, String targetType,
                                Long targetId, String conditionConfig, String actionConfig,
                                String notificationChannels, Integer isEnabled) {
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> r.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("告警规则不存在: " + id));
        if (ruleName != null) rule.setRuleName(ruleName);
        if (ruleType != null) rule.setRuleType(ruleType);
        if (targetType != null) rule.setTargetType(targetType);
        if (targetId != null) rule.setTargetId(targetId);
        if (conditionConfig != null) rule.setConditionConfig(conditionConfig);
        if (actionConfig != null) rule.setActionConfig(actionConfig);
        if (notificationChannels != null) rule.setNotificationChannels(notificationChannels);
        if (isEnabled != null) rule.setIsEnabled(isEnabled);
        return alertRuleRepository.save(rule);
    }

    public AlertRule toggleEnabled(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> r.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("告警规则不存在: " + id));
        rule.setIsEnabled(rule.getIsEnabled() == 1 ? 0 : 1);
        return alertRuleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> r.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("告警规则不存在: " + id));
        rule.setDeleted(1);
        alertRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public PageResult<AlertHistory> listHistories(int page, int size) {
        Page<AlertHistory> pageResult = alertHistoryRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page - 1, size));
        return PageResult.<AlertHistory>builder()
                .records(pageResult.getContent())
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AlertRule> listEnabledRules() {
        return alertRuleRepository.findByIsEnabledAndDeleted(1, 0);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return alertHistoryRepository.countByCreatedAtAfter(
                java.time.LocalDateTime.now().minusHours(24));
    }

    @Transactional(readOnly = true)
    public List<AlertHistory> getRecentAlerts() {
        return alertHistoryRepository.findTop10ByOrderByCreatedAtDesc();
    }
}
