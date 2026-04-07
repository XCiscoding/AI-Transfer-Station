package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.dto.project.ProjectCreateRequest;
import com.aikey.dto.project.ProjectUpdateRequest;
import com.aikey.dto.project.ProjectVO;
import com.aikey.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "项目管理", description = "项目的增删改查接口")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "项目列表")
    public Result<PageResult<ProjectVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long teamId) {
        return Result.success(projectService.listProjects(page, size, keyword, teamId));
    }

    @PostMapping
    @Operation(summary = "创建项目")
    public Result<ProjectVO> create(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目")
    public Result<ProjectVO> update(@PathVariable Long id, @RequestBody ProjectUpdateRequest request) {
        return Result.success(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.success();
    }
}
