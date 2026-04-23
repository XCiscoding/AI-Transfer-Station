package com.aikey.service;

import com.aikey.dto.auth.UserInfoResponse;
import com.aikey.dto.common.PageResult;
import com.aikey.entity.AlertHistory;
import com.aikey.entity.AlertRule;
import com.aikey.entity.User;
import com.aikey.exception.BusinessException;
import com.aikey.repository.AlertHistoryRepository;
import com.aikey.repository.AlertRuleRepository;
import com.aikey.repository.ProjectRepository;
import com.aikey.repository.TeamMemberRepository;
import com.aikey.repository.TeamRepository;
import com.aikey.repository.UserRepository;
import com.aikey.repository.VirtualKeyRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final VirtualKeyRepository virtualKeyRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public PageResult<AlertRule> listRules(int page, int size) {
        requireSuperAdmin();
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
        requireSuperAdmin();
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
        requireSuperAdmin();
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
        requireSuperAdmin();
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> r.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("告警规则不存在: " + id));
        rule.setIsEnabled(rule.getIsEnabled() == 1 ? 0 : 1);
        return alertRuleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        requireSuperAdmin();
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> r.getDeleted() == 0)
                .orElseThrow(() -> new RuntimeException("告警规则不存在: " + id));
        rule.setDeleted(1);
        alertRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public PageResult<AlertHistory> listHistories(int page, int size) {
        Page<AlertHistory> pageResult = alertHistoryRepository.findAll(
                buildVisibleHistorySpec(null),
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
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
        return alertHistoryRepository.count(buildVisibleHistorySpec(LocalDateTime.now().minusHours(24)));
    }

    @Transactional(readOnly = true)
    public List<AlertHistory> getRecentAlerts() {
        return alertHistoryRepository.findAll(
                buildVisibleHistorySpec(null),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    @Transactional(readOnly = true)
    public void assertRuleManagementAllowed() {
        requireSuperAdmin();
    }

    private Specification<AlertHistory> buildVisibleHistorySpec(LocalDateTime createdAfter) {
        User currentUser = getCurrentUser();
        if (isSuperAdmin(currentUser)) {
            return (root, query, cb) -> createdAfter == null
                    ? cb.conjunction()
                    : cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
        }

        List<Long> teamIds = getVisibleTeamIds(currentUser);
        List<Long> projectIds = teamIds.isEmpty()
                ? List.of()
                : projectRepository.findByTeamIdInAndDeleted(teamIds, 0).stream()
                        .map(project -> project.getId())
                        .toList();
        List<Long> virtualKeyIds = new ArrayList<>();
        virtualKeyIds.addAll(virtualKeyRepository.findByUserIdAndDeleted(currentUser.getId(), 0).stream()
                .map(key -> key.getId())
                .toList());
        if (!teamIds.isEmpty()) {
            virtualKeyIds.addAll(virtualKeyRepository.findByTeamIdInAndDeleted(teamIds, 0).stream()
                    .map(key -> key.getId())
                    .toList());
        }
        List<Long> distinctVirtualKeyIds = virtualKeyIds.stream().distinct().toList();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }

            List<Predicate> scopePredicates = new ArrayList<>();
            if (!teamIds.isEmpty()) {
                scopePredicates.add(cb.and(root.get("targetType").in("team"), root.get("targetId").in(teamIds)));
            }
            if (!projectIds.isEmpty()) {
                scopePredicates.add(cb.and(root.get("targetType").in("project"), root.get("targetId").in(projectIds)));
            }
            if (!distinctVirtualKeyIds.isEmpty()) {
                scopePredicates.add(cb.and(root.get("targetType").in("virtual_key", "token", "key"), root.get("targetId").in(distinctVirtualKeyIds)));
            }
            scopePredicates.add(cb.and(cb.equal(root.get("targetType"), "user"), cb.equal(root.get("targetId"), currentUser.getId())));

            if (scopePredicates.isEmpty()) {
                predicates.add(cb.disjunction());
            } else {
                predicates.add(cb.or(scopePredicates.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void requireSuperAdmin() {
        if (!isSuperAdmin(getCurrentUser())) {
            throw new BusinessException(403, "仅企业管理员可设置告警规则");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new BusinessException(401, "未获取到当前登录用户");
        }
        return userRepository.findWithRolesByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException(401, "当前登录用户不存在"));
    }

    private boolean isSuperAdmin(User user) {
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return false;
        }
        UserInfoResponse userInfo = authService.getUserInfo(user.getUsername());
        return Boolean.TRUE.equals(userInfo.getIsSuperAdmin());
    }

    private List<Long> getVisibleTeamIds(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        if (teamRepository.existsByOwnerIdAndDeleted(user.getId(), 0)) {
            return teamRepository.findByOwnerIdAndDeleted(user.getId(), 0).stream()
                    .map(team -> team.getId())
                    .toList();
        }
        return teamMemberRepository.findByUserIdOrderByJoinedAtAsc(user.getId()).stream()
                .map(member -> member.getTeam().getId())
                .distinct()
                .toList();
    }
}
