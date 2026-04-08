package com.aikey.controller;

import com.aikey.dto.calllog.CallLogQueryRequest;
import com.aikey.dto.calllog.CallLogVO;
import com.aikey.dto.common.PageResult;
import com.aikey.dto.common.Result;
import com.aikey.service.CallLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 调用日志查询接口
 */
@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "调用日志", description = "API调用日志查询接口")
@RequiredArgsConstructor
public class CallLogController {

    private final CallLogQueryService callLogQueryService;

    @GetMapping
    @Operation(summary = "分页查询调用日志", description = "支持按时间范围、模型名称、状态、渠道、虚拟Key、用户筛选")
    public Result<PageResult<CallLogVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Long virtualKeyId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        CallLogQueryRequest req = new CallLogQueryRequest();
        req.setStartTime(startTime);
        req.setEndTime(endTime);
        req.setModelName(modelName);
        req.setStatus(status);
        req.setChannelId(channelId);
        req.setVirtualKeyId(virtualKeyId);
        req.setUserId(userId);
        req.setPage(page);
        req.setSize(size);

        return Result.success(callLogQueryService.queryLogs(req));
    }
}
