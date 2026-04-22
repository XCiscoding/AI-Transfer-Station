package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.entity.AlertHistory;
import com.aikey.entity.AlertRule;
import com.aikey.service.AlertCheckService;
import com.aikey.service.AlertRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alert-rules")
@Tag(name = "告警规则", description = "告警规则的增删改查及历史查询")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final AlertCheckService alertCheckService;

    @GetMapping
    @Operation(summary = "分页查询告警规则")
    public Result<PageResult<AlertRule>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(alertRuleService.listRules(page, size));
    }

    @PostMapping
    @Operation(summary = "创建告警规则")
    public Result<AlertRule> create(@RequestBody AlertRuleRequest req) {
        return Result.success(alertRuleService.createRule(
                req.getRuleName(), req.getRuleType(), req.getTargetType(), req.getTargetId(),
                req.getConditionConfig(), req.getActionConfig(),
                req.getNotificationChannels(), req.getIsEnabled()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新告警规则")
    public Result<AlertRule> update(@PathVariable Long id, @RequestBody AlertRuleRequest req) {
        return Result.success(alertRuleService.updateRule(
                id, req.getRuleName(), req.getRuleType(), req.getTargetType(), req.getTargetId(),
                req.getConditionConfig(), req.getActionConfig(),
                req.getNotificationChannels(), req.getIsEnabled()));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "切换告警规则启用状态")
    public Result<AlertRule> toggle(@PathVariable Long id) {
        return Result.success(alertRuleService.toggleEnabled(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除告警规则（软删除）")
    public Result<Void> delete(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return Result.success();
    }

    @GetMapping("/histories")
    @Operation(summary = "分页查询告警历史")
    public Result<PageResult<AlertHistory>> histories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(alertRuleService.listHistories(page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "查询未读告警数（24小时内）")
    public Result<Long> unreadCount() {
        return Result.success(alertRuleService.getUnreadCount());
    }

    @GetMapping("/recent")
    @Operation(summary = "查询最近10条告警历史")
    public Result<List<AlertHistory>> recent() {
        return Result.success(alertRuleService.getRecentAlerts());
    }

    @PostMapping("/check-now")
    @Operation(summary = "立即触发一次告警检查（测试用）")
    public Result<String> checkNow() {
        alertCheckService.checkAlertRules();
        return Result.success("告警检查已执行，请查看历史记录");
    }

    @Data
    public static class AlertRuleRequest {
        private String ruleName;
        private String ruleType;
        private String targetType;
        private Long targetId;
        private String conditionConfig;
        private String actionConfig;
        private String notificationChannels;
        private Integer isEnabled;
    }
}
