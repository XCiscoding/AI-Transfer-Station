package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.dto.realkey.RealKeyCreateRequest;
import com.aikey.dto.realkey.RealKeyUpdateRequest;
import com.aikey.dto.realkey.RealKeyVO;
import com.aikey.service.RealKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 真实Key管理控制器
 *
 * <p>提供真实API Key的增删改查接口</p>
 */
@RestController
@RequestMapping("/api/v1/real-keys")
@Tag(name = "真实Key管理", description = "真实API Key的增删改查接口")
@RequiredArgsConstructor
public class RealKeyController {

    private final RealKeyService realKeyService;

    /**
     * 录入真实Key
     *
     * @param request 录入请求（包含明文Key值，后端自动AES加密）
     * @return 录入成功的真实Key VO
     */
    @PostMapping
    @Operation(summary = "录入真实Key", description = "录入新的真实API Key，Key值将被AES加密存储")
    public Result<RealKeyVO> create(@Valid @RequestBody RealKeyCreateRequest request) {
        return Result.success(realKeyService.createRealKey(request));
    }

    /**
     * 查询真实Key详情
     *
     * @param id Key ID
     * @return 真实Key VO（含掩码显示）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询Key详情", description = "根据ID查询真实Key详情")
    public Result<RealKeyVO> getById(@PathVariable Long id) {
        return Result.success(realKeyService.getRealKeyById(id));
    }

    /**
     * 分页查询真实Key列表
     *
     * @param page      页码（从1开始，默认1）
     * @param size      每页大小（默认10）
     * @param channelId 渠道ID筛选（可选）
     * @param keyword   关键词搜索（可选，匹配Key名称）
     * @return 分页的真实Key列表
     */
    @GetMapping
    @Operation(summary = "Key列表", description = "分页查询真实Key列表，支持渠道筛选和关键词搜索")
    public Result<PageResult<RealKeyVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String keyword) {
        return Result.success(realKeyService.listRealKeys(page, size, channelId, keyword));
    }

    /**
     * 更新真实Key信息
     *
     * @param id      Key ID
     * @param request 更新请求（只更新非空字段）
     * @return 更新后的真实Key VO
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新Key信息", description = "更新真实Key的基本信息")
    public Result<RealKeyVO> update(
            @PathVariable Long id,
            @Valid @RequestBody RealKeyUpdateRequest request) {
        return Result.success(realKeyService.updateRealKey(id, request));
    }

    /**
     * 切换真实Key启用/禁用状态
     *
     * @param id Key ID
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "切换Key状态", description = "启用/禁用真实Key")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        realKeyService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 删除真实Key（逻辑删除）
     *
     * @param id Key ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除Key", description = "逻辑删除真实Key")
    public Result<Void> delete(@PathVariable Long id) {
        realKeyService.deleteRealKey(id);
        return Result.success();
    }
}
