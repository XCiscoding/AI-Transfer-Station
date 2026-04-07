package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.dto.model.ModelCreateRequest;
import com.aikey.dto.model.ModelVO;
import com.aikey.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模型广场控制器
 */
@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "模型广场", description = "模型的增删改查接口")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    @Operation(summary = "模型列表", description = "分页查询模型列表，支持关键词/类型/渠道筛选")
    public Result<PageResult<ModelVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) Long channelId) {
        return Result.success(modelService.listModels(page, size, keyword, modelType, channelId));
    }

    @PostMapping
    @Operation(summary = "创建模型", description = "新增模型记录")
    public Result<ModelVO> create(@Valid @RequestBody ModelCreateRequest request) {
        return Result.success(modelService.createModel(request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换模型状态", description = "启用/下线模型")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        modelService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型", description = "逻辑删除模型")
    public Result<Void> delete(@PathVariable Long id) {
        modelService.deleteModel(id);
        return Result.success();
    }
}
