package com.aikey.controller;

import com.aikey.dto.common.Result;
import com.aikey.dto.team.TeamMemberAddRequest;
import com.aikey.dto.team.TeamMemberVO;
import com.aikey.dto.team.TeamOwnerTransferRequest;
import com.aikey.service.TeamMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}")
@Tag(name = "团队成员管理", description = "团队成员查询和维护接口")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    @GetMapping("/members")
    @Operation(summary = "团队成员列表")
    public Result<List<TeamMemberVO>> listMembers(@PathVariable Long teamId) {
        return Result.success(teamMemberService.listMembers(teamId));
    }

    @GetMapping("/member-candidates")
    @Operation(summary = "团队候选成员列表")
    public Result<List<TeamMemberService.UserCandidateVO>> listCandidates(
            @PathVariable Long teamId,
            @RequestParam(required = false) String keyword) {
        return Result.success(teamMemberService.listCandidates(teamId, keyword));
    }

    @PostMapping("/members")
    @Operation(summary = "添加团队成员")
    public Result<List<TeamMemberVO>> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamMemberAddRequest request) {
        return Result.success(teamMemberService.addMember(teamId, request));
    }

    @DeleteMapping("/members/{userId}")
    @Operation(summary = "移除团队成员")
    public Result<List<TeamMemberVO>> removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        return Result.success(teamMemberService.removeMember(teamId, userId));
    }

    @PutMapping("/owner")
    @Operation(summary = "转交团队管理员")
    public Result<List<TeamMemberVO>> transferOwner(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamOwnerTransferRequest request) {
        return Result.success(teamMemberService.transferOwner(teamId, request));
    }
}
