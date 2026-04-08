package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.dto.quota.QuotaAdjustRequest;
import com.aikey.dto.quota.QuotaRechargeRequest;
import com.aikey.dto.quota.QuotaResetRequest;
import com.aikey.dto.quota.QuotaSummaryVO;
import com.aikey.dto.quota.QuotaTransactionVO;
import com.aikey.exception.BusinessException;
import com.aikey.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 额度管理接口
 */
@RestController
@RequestMapping("/api/v1/quota")
@Tag(name = "额度管理", description = "充值、调整、重置、余额查询、流水查询")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    @PostMapping("/recharge")
    @Operation(summary = "充值额度")
    public Result<Void> recharge(@Valid @RequestBody QuotaRechargeRequest req) {
        switch (req.getTargetType()) {
            case "virtual_key" -> quotaService.rechargeVirtualKey(req.getTargetId(), null, req.getAmount(), req.getDescription());
            case "team"        -> quotaService.rechargeTeam(req.getTargetId(), null, req.getAmount(), req.getDescription());
            case "project"     -> quotaService.rechargeProject(req.getTargetId(), null, req.getAmount(), req.getDescription());
            default            -> throw new BusinessException("不支持的目标类型：" + req.getTargetType());
        }
        return Result.success();
    }

    @PostMapping("/adjust")
    @Operation(summary = "调整额度（正=增加，负=减少）")
    public Result<Void> adjust(@Valid @RequestBody QuotaAdjustRequest req) {
        switch (req.getTargetType()) {
            case "virtual_key" -> quotaService.adjustVirtualKeyQuota(req.getTargetId(), null, req.getAmount(), req.getDescription());
            case "team" -> {
                // 团队暂时只支持增加（通过充值实现）
                if (req.getAmount().signum() > 0) {
                    quotaService.rechargeTeam(req.getTargetId(), null, req.getAmount(), req.getDescription());
                } else {
                    throw new BusinessException("团队额度暂不支持减少操作，请联系管理员");
                }
            }
            case "project" -> {
                if (req.getAmount().signum() > 0) {
                    quotaService.rechargeProject(req.getTargetId(), null, req.getAmount(), req.getDescription());
                } else {
                    throw new BusinessException("项目额度暂不支持减少操作，请联系管理员");
                }
            }
            default -> throw new BusinessException("不支持的目标类型：" + req.getTargetType());
        }
        return Result.success();
    }

    @PostMapping("/reset")
    @Operation(summary = "重置额度（清零已用，剩余=上限）")
    public Result<Void> reset(@Valid @RequestBody QuotaResetRequest req) {
        switch (req.getTargetType()) {
            case "virtual_key" -> quotaService.resetVirtualKeyQuota(req.getTargetId(), null);
            case "team"        -> quotaService.resetTeamQuota(req.getTargetId(), null);
            case "project"     -> quotaService.resetProjectQuota(req.getTargetId(), null);
            default            -> throw new BusinessException("不支持的目标类型：" + req.getTargetType());
        }
        return Result.success();
    }

    @GetMapping("/summary")
    @Operation(summary = "查询余额摘要")
    public Result<QuotaSummaryVO> summary(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return Result.success(quotaService.getSummary(targetType, targetId));
    }

    @GetMapping("/transactions")
    @Operation(summary = "分页查询额度流水")
    public Result<PageResult<QuotaTransactionVO>> transactions(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(quotaService.listTransactions(targetType, targetId, userId, page, size));
    }
}
