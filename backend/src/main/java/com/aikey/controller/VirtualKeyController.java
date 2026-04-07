package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.dto.virtualkey.VirtualKeyCreateRequest;
import com.aikey.dto.virtualkey.VirtualKeyUpdateRequest;
import com.aikey.dto.virtualkey.VirtualKeyVO;
import com.aikey.service.VirtualKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 虚拟Key管理控制器
 *
 * <p>提供虚拟API Key的增删改查、状态切换、刷新等REST接口</p>
 */
@RestController
@RequestMapping("/api/v1/virtual-keys")
@Tag(name = "虚拟Key管理", description = "虚拟API Key的增删改查、状态切换、刷新接口")
@RequiredArgsConstructor
public class VirtualKeyController {

    private final VirtualKeyService virtualKeyService;

    /**
     * 生成虚拟Key
     *
     * @param request 创建请求（包含用户绑定、额度配置、限速配置等）
     * @return 生成的虚拟Key VO（包含完整Key值）
     */
    @PostMapping
    @Operation(summary = "生成虚拟Key",
            description = "生成新的虚拟API Key，自动生成sk-xxx格式的唯一Key值，支持配置额度和限速策略")
    public Result<VirtualKeyVO> create(@Valid @RequestBody VirtualKeyCreateRequest request) {
        return Result.success(virtualKeyService.createVirtualKey(request));
    }

    /**
     * 查询虚拟Key详情
     *
     * @param id Key ID
     * @return 虚拟Key VO（包含完整Key值和关联信息）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询Key详情", description = "根据ID查询虚拟Key详情，返回完整信息包括关联用户名")
    public Result<VirtualKeyVO> getById(@PathVariable Long id) {
        return Result.success(virtualKeyService.getVirtualKeyById(id));
    }

    /**
     * 分页查询虚拟Key列表
     *
     * @param page      页码（从1开始，默认1）
     * @param size      每页大小（默认10）
     * @param userId    用户ID筛选（可选）
     * @param status    状态筛选（可选，0-禁用，1-启用）
     * @param keyword   关键词搜索（可选，匹配Key名称）
     * @return 分页的虚拟Key列表
     */
    @GetMapping
    @Operation(summary = "虚拟Key列表",
            description = "分页查询虚拟Key列表，支持按用户、状态筛选和关键词搜索")
    public Result<PageResult<VirtualKeyVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(virtualKeyService.listVirtualKeys(page, size, userId, status, keyword));
    }

    /**
     * 更新虚拟Key配置
     *
     * @param id      Key ID
     * @param request 更新请求（只更新非空字段）
     * @return 更新后的虚拟Key VO
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新Key配置", description = "更新虚拟Key的基本信息和配置参数")
    public Result<VirtualKeyVO> update(
            @PathVariable Long id,
            @Valid @RequestBody VirtualKeyUpdateRequest request) {
        return Result.success(virtualKeyService.updateVirtualKey(id, request));
    }

    /**
     * 刷新虚拟Key值（重新生成）
     *
     * <p>重新生成sk-xxx格式的Key值，保持其他所有配置不变</p>
     *
     * @param id Key ID
     * @return 更新后的虚拟Key VO（包含新的Key值）
     */
    @PutMapping("/{id}/refresh")
    @Operation(summary = "刷新Key值",
            description = "重新生成虚拟Key值（sk-xxx格式），旧Key立即失效，其他配置保持不变")
    public Result<VirtualKeyVO> refresh(@PathVariable Long id) {
        return Result.success(virtualKeyService.refreshKey(id));
    }

    /**
     * 切换虚拟Key启用/禁用状态
     *
     * @param id Key ID
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "切换Key状态", description = "启用或禁用虚拟Key（0-禁用，1-启用）")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        virtualKeyService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 删除虚拟Key（逻辑删除）
     *
     * @param id Key ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除Key", description = "逻辑删除虚拟Key（标记为已删除，不物理删除）")
    public Result<Void> delete(@PathVariable Long id) {
        virtualKeyService.deleteVirtualKey(id);
        return Result.success();
    }
}
