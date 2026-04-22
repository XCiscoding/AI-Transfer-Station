package com.aikey.controller;

import com.aikey.dto.common.Result;
import com.aikey.dto.dashboard.DashboardOverviewVO;
import com.aikey.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "首页看板", description = "概览统计数据接口")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "获取今日概览统计数据")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.getOverview());
    }
}
