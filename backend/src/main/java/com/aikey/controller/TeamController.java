package com.aikey.controller;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.dto.team.TeamCreateRequest;
import com.aikey.dto.team.TeamUpdateRequest;
import com.aikey.dto.team.TeamVO;
import com.aikey.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "团队管理", description = "团队的增删改查接口")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @Operation(summary = "团队列表")
    public Result<PageResult<TeamVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(teamService.listTeams(page, size, keyword));
    }

    @PostMapping
    @Operation(summary = "创建团队")
    public Result<TeamVO> create(@Valid @RequestBody TeamCreateRequest request) {
        return Result.success(teamService.createTeam(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新团队")
    public Result<TeamVO> update(@PathVariable Long id, @RequestBody TeamUpdateRequest request) {
        return Result.success(teamService.updateTeam(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除团队")
    public Result<Void> delete(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return Result.success();
    }
}
