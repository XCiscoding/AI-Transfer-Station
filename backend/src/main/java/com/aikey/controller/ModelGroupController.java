package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.dto.modelgroup.ModelGroupCreateRequest;
import com.aikey.dto.modelgroup.ModelGroupUpdateRequest;
import com.aikey.dto.modelgroup.ModelGroupVO;
import com.aikey.service.ModelGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型分组管理接口
 */
@RestController
@RequestMapping("/api/v1/model-groups")
@Tag(name = "模型分组", description = "模型分组的增删改查接口")
@RequiredArgsConstructor
public class ModelGroupController {

    private final ModelGroupService modelGroupService;

    @GetMapping
    @Operation(summary = "分页查询模型分组列表")
    public Result<PageResult<ModelGroupVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(modelGroupService.list(page, size, keyword));
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有启用分组（下拉选择用）")
    public Result<List<ModelGroupVO>> listAll() {
        return Result.success(modelGroupService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模型分组详情")
    public Result<ModelGroupVO> getById(@PathVariable Long id) {
        return Result.success(modelGroupService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建模型分组")
    public Result<ModelGroupVO> create(@Valid @RequestBody ModelGroupCreateRequest request) {
        return Result.success(modelGroupService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型分组")
    public Result<ModelGroupVO> update(@PathVariable Long id,
                                       @RequestBody ModelGroupUpdateRequest request) {
        return Result.success(modelGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除模型分组")
    public Result<Void> delete(@PathVariable Long id) {
        modelGroupService.delete(id);
        return Result.success();
    }
}
