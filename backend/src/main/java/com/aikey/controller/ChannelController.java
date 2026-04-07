package com.aikey.controller;

import com.aikey.dto.channel.ChannelCreateRequest;
import com.aikey.dto.channel.ChannelUpdateRequest;
import com.aikey.dto.channel.ChannelVO;
import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 渠道管理控制器
 *
 * <p>提供AI厂商渠道的增删改查接口</p>
 */
@RestController
@RequestMapping("/api/v1/channels")
@Tag(name = "渠道管理", description = "AI厂商渠道的增删改查接口")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    /**
     * 创建渠道
     *
     * @param request 创建请求（包含渠道基本信息和明文API Key）
     * @return 创建成功的渠道VO
     */
    @PostMapping
    @Operation(summary = "创建渠道", description = "创建新的AI厂商渠道，API Key将被AES加密存储")
    public Result<ChannelVO> create(@Valid @RequestBody ChannelCreateRequest request) {
        return Result.success(channelService.createChannel(request));
    }

    /**
     * 查询渠道列表（分页）
     *
     * @param page        页码（从1开始，默认1）
     * @param size        每页大小（默认10）
     * @param keyword     关键词搜索（可选，匹配渠道名称或编码）
     * @param channelType 渠道类型筛选（可选）
     * @return 分页的渠道列表
     */
    @GetMapping
    @Operation(summary = "渠道列表", description = "分页查询渠道列表，支持关键词搜索和类型筛选")
    public Result<PageResult<ChannelVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channelType) {
        return Result.success(channelService.listChannels(page, size, keyword, channelType));
    }

    /**
     * 更新渠道信息
     *
     * @param id      渠道ID
     * @param request 更新请求（只更新非空字段）
     * @return 更新后的渠道VO
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新渠道", description = "更新渠道基本信息")
    public Result<ChannelVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ChannelUpdateRequest request) {
        return Result.success(channelService.updateChannel(id, request));
    }

    /**
     * 删除渠道（逻辑删除）
     *
     * @param id 渠道ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除渠道", description = "逻辑删除渠道")
    public Result<Void> delete(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return Result.success();
    }

    /**
     * 切换渠道启用/禁用状态
     *
     * @param id 渠道ID
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "切换渠道状态", description = "启用/禁用渠道")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        channelService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 测试渠道API连通性
     *
     * @param id 渠道ID
     * @return 连通性测试结果（true=成功，false=失败）
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "测试连通性", description = "测试渠道API连接是否可用")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        return Result.success(channelService.testConnection(id).join());
    }
}
