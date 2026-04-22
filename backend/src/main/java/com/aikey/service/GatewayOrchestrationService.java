package com.aikey.service;

import com.aikey.dto.gateway.ChatCompletionRequest;
import com.aikey.dto.gateway.ChatCompletionResponse;
import com.aikey.dto.gateway.UsageInfo;
import com.aikey.entity.CallLog;
import com.aikey.entity.Channel;
import com.aikey.entity.Model;
import com.aikey.entity.RealKey;
import com.aikey.entity.VirtualKey;
import com.aikey.exception.BusinessException;
import com.aikey.gateway.auth.VirtualKeyAuthContext;
import com.aikey.gateway.dispatch.DispatchResult;
import com.aikey.gateway.dispatch.DispatchService;
import com.aikey.gateway.dispatch.DispatchStrategyType;
import com.aikey.gateway.proxy.ProxyForwardService;
import com.aikey.gateway.ratelimit.RateLimitService;
import com.aikey.repository.ChannelRepository;
import com.aikey.repository.ModelRepository;
import com.aikey.repository.RealKeyRepository;
import com.aikey.repository.VirtualKeyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 网关编排服务
 *
 * <p>实现完整的调用链路：
 * 鉴权(Filter) → 权限检查 → 限流 → 额度预检 → 调度 → 转发 → 额度扣减 → 日志记录 → 返回结果</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayOrchestrationService {

    private final RateLimitService rateLimitService;
    private final QuotaService quotaService;
    private final DispatchService dispatchService;
    private final ProxyForwardService proxyForwardService;
    private final CallLogService callLogService;
    private final ChannelRepository channelRepository;
    private final ModelRepository modelRepository;
    private final RealKeyRepository realKeyRepository;
    private final VirtualKeyRepository virtualKeyRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY = 3;

    /**     * 处理流式 Chat Completion 请求（SSE）
     *
     * <p>同步执行鉴权、权限、限流、额度预检、调度，然后异步转发流式响应。</p>
     *
     * @param request   请求体
     * @param clientIp  客户端 IP
     * @param userAgent 客户端 User-Agent
     * @return SseEmitter，由 Spring MVC 自动处理流式响应
     */
    public SseEmitter processChatCompletionStream(ChatCompletionRequest request,
                                                   String clientIp,
                                                   String userAgent) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();

        // 必须在主线程（ThreadLocal 有效期内）获取虚拟Key
        VirtualKey virtualKey = VirtualKeyAuthContext.get();
        if (virtualKey == null) {
            throw new BusinessException(401, "Authentication required.");
        }

        // 前置检查（同步）
        checkModelPermission(virtualKey, request.getModel());
        rateLimitService.checkRateLimit(virtualKey);
        quotaService.checkQuotaWithFunnel(virtualKey);

        // 调度（同步，目前流式不做故障转移）
        Set<Long> excludedChannelIds = new HashSet<>();
        DispatchResult dispatchResult = resolveDispatchResult(virtualKey, request.getModel(), excludedChannelIds);

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // final 引用，供 lambda 捕获
        final VirtualKey finalVirtualKey = virtualKey;
        final DispatchResult finalDispatch = dispatchResult;

        CompletableFuture.runAsync(() -> {
            try {
                UsageInfo usage = proxyForwardService.forwardChatCompletionStream(
                        finalDispatch.getChannel().getBaseUrl(),
                        finalDispatch.getChannel().getChannelType(),
                        finalDispatch.getRealKey(),
                        request,
                        emitter
                );

                long elapsed = System.currentTimeMillis() - startTime;

                // 记录渠道成功统计
                dispatchService.recordSuccess(finalDispatch.getChannel(), (int) elapsed);
                try {
                    channelRepository.save(finalDispatch.getChannel());
                } catch (Exception e) {
                    log.warn("保存渠道统计失败: {}", e.getMessage());
                }

                // 额度扣减 + 日志记录
                ChatCompletionResponse synthetic = new ChatCompletionResponse();
                synthetic.setUsage(usage);
                handleSuccess(finalVirtualKey, finalDispatch, synthetic, traceId, clientIp, userAgent, (int) elapsed, request.getModel());

            } catch (BusinessException be) {
                long elapsed = System.currentTimeMillis() - startTime;
                dispatchService.recordFailure(finalDispatch.getChannel());
                try {
                    channelRepository.save(finalDispatch.getChannel());
                } catch (Exception e) {
                    log.warn("保存渠道失败统计异常: {}", e.getMessage());
                }
                handleFailure(finalVirtualKey, finalDispatch, be, traceId, clientIp, userAgent, (int) elapsed, request.getModel());
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"error\":{\"message\":\"" + be.getMessage() + "\",\"code\":" + be.getCode() + "}}"));
                    emitter.complete();
                } catch (Exception ignored) { }
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                dispatchService.recordFailure(finalDispatch.getChannel());
                try {
                    channelRepository.save(finalDispatch.getChannel());
                } catch (Exception ex) {
                    log.warn("保存渠道失败统计异常: {}", ex.getMessage());
                }
                handleFailure(finalVirtualKey, finalDispatch, e, traceId, clientIp, userAgent, (int) elapsed, request.getModel());
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) { }
            }
        });

        return emitter;
    }

    /**     * 处理Chat Completion请求的完整链路
     *
     * @param request   请求体
     * @param clientIp  客户端IP
     * @param userAgent 客户端User-Agent
     * @return 厂商响应
     */
    public ChatCompletionResponse processChatCompletion(ChatCompletionRequest request,
                                                         String clientIp,
                                                         String userAgent) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();

        VirtualKey virtualKey = VirtualKeyAuthContext.get();
        if (virtualKey == null) {
            throw new BusinessException(401, "Authentication required.");
        }

        // 1. 模型权限检查
        checkModelPermission(virtualKey, request.getModel());

        // 3. 限流检查
        rateLimitService.checkRateLimit(virtualKey);

        // 4. 额度预检查（三层漏斗）
        quotaService.checkQuotaWithFunnel(virtualKey);

        // 5. 故障转移循环
        Set<Long> excludedChannelIds = new HashSet<>();
        DispatchResult dispatchResult = null;
        ChatCompletionResponse response = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                dispatchResult = resolveDispatchResult(virtualKey, request.getModel(), excludedChannelIds);

                // 转发
                response = proxyForwardService.forwardChatCompletion(
                        dispatchResult.getChannel().getBaseUrl(),
                    dispatchResult.getChannel().getChannelType(),
                        dispatchResult.getRealKey(),
                        request
                );

                // 成功
                long elapsed = System.currentTimeMillis() - startTime;
                dispatchService.recordSuccess(dispatchResult.getChannel(), (int) elapsed);

                // 保存渠道统计
                try {
                    channelRepository.save(dispatchResult.getChannel());
                } catch (Exception e) {
                    log.warn("保存渠道统计失败: {}", e.getMessage());
                }

                break;

            } catch (BusinessException e) {
                lastException = e;
                // 4xx错误不重试（客户端错误）
                if (e.getCode() >= 400 && e.getCode() < 500 && e.getCode() != 429) {
                    break;
                }
                // 503（无可用渠道）不重试
                if (e.getCode() == 503) {
                    break;
                }
                // 5xx / 429 / 502 / 504 可以重试
                if (dispatchResult != null) {
                    excludedChannelIds.add(dispatchResult.getChannel().getId());
                    dispatchService.recordFailure(dispatchResult.getChannel());
                    try {
                        channelRepository.save(dispatchResult.getChannel());
                    } catch (Exception ex) {
                        log.warn("保存渠道失败统计异常: {}", ex.getMessage());
                    }
                    log.warn("第{}次尝试失败，排除渠道 {}，准备重试: {}",
                            attempt, dispatchResult.getChannel().getChannelName(), e.getMessage());
                }
            }
        }

        // 6. 处理结果
        long totalElapsed = System.currentTimeMillis() - startTime;

        if (response != null) {
            // 成功：额度扣减 + 日志记录
            handleSuccess(virtualKey, dispatchResult, response, traceId, clientIp, userAgent, (int) totalElapsed, request.getModel());
            return response;
        } else {
            // 失败：记录失败日志
            handleFailure(virtualKey, dispatchResult, lastException, traceId, clientIp, userAgent, (int) totalElapsed, request.getModel());
            throw lastException instanceof BusinessException
                    ? (BusinessException) lastException
                    : new BusinessException(502, "All channels failed for model: " + request.getModel());
        }
    }

    /**
     * 检查模型权限
     */
    private void checkModelPermission(VirtualKey virtualKey, String requestedModel) {
        String allowedModels = virtualKey.getAllowedModels();
        if (!StringUtils.hasText(allowedModels) || "[]".equals(allowedModels.trim())) {
            return; // 不限制
        }

        try {
            List<String> modelList = objectMapper.readValue(allowedModels, new TypeReference<List<String>>() {});
            if (modelList.isEmpty()) {
                return;
            }
            if (!modelList.contains(requestedModel)) {
                throw new BusinessException(403, "Model '" + requestedModel + "' is not allowed for this API key.");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析allowedModels失败: {}", e.getMessage());
            // 解析失败视为不限制
        }
    }

    private DispatchResult resolveDispatchResult(VirtualKey virtualKey, String modelCode, Set<Long> excludedChannelIds) {
        if (virtualKey.getChannelId() == null) {
            return dispatchService.dispatch(
                    modelCode,
                    DispatchStrategyType.WEIGHTED,
                    excludedChannelIds
            );
        }
        return dispatchByAssignedChannel(virtualKey.getChannelId(), modelCode, excludedChannelIds);
    }

    private DispatchResult dispatchByAssignedChannel(Long channelId, String modelCode, Set<Long> excludedChannelIds) {
        if (excludedChannelIds != null && excludedChannelIds.contains(channelId)) {
            throw new BusinessException(503, "Assigned channel is temporarily unavailable.");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new BusinessException(404, "Assigned channel not found."));
        if (channel.getDeleted() == 1 || channel.getStatus() != 1) {
            throw new BusinessException(503, "Assigned channel is unavailable.");
        }
        if (channel.getHealthStatus() != null && channel.getHealthStatus() != 1) {
            throw new BusinessException(503, "Assigned channel is unhealthy.");
        }

        Model model = modelRepository.findByModelCodeAndChannelIdAndDeleted(modelCode, channelId, 0).stream()
                .filter(item -> item.getStatus() == 1)
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, "Assigned channel does not support model: " + modelCode));

        List<RealKey> realKeys = realKeyRepository.findByChannelIdAndDeletedOrderByCreatedAtDesc(channelId, 0).stream()
                .filter(realKey -> realKey.getStatus() == 1)
                .filter(realKey -> realKey.getExpireTime() == null || realKey.getExpireTime().isAfter(LocalDateTime.now()))
                .toList();
        if (realKeys.isEmpty()) {
            throw new BusinessException(503, "Assigned channel has no available real key.");
        }

        RealKey selectedKey = realKeys.get(0);
        return DispatchResult.builder()
                .channel(channel)
                .model(model)
                .realKey(selectedKey)
                .strategyUsed(DispatchStrategyType.WEIGHTED)
                .build();
    }

    private void handleSuccess(VirtualKey virtualKey, DispatchResult dispatchResult,
                                ChatCompletionResponse response, String traceId,
                                String clientIp, String userAgent, int responseTime, String requestModel) {
        // 加权额度扣减
        UsageInfo usage = response.getUsage();
        if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
            BigDecimal rawTokens = BigDecimal.valueOf(usage.getTotalTokens());

            // 获取三层倍率
            BigDecimal modelWeight = dispatchResult.getModel().getQuotaWeight();
            BigDecimal teamWeight = quotaService.getTeamWeight(virtualKey.getTeamId());
            BigDecimal projectWeight = quotaService.getProjectWeight(virtualKey.getProjectId());

            // 计算加权消耗量
            BigDecimal weightedAmount = quotaService.calculateWeightedAmount(
                    rawTokens, modelWeight, teamWeight, projectWeight);

            // 三层同步扣减（含流水记录）
            quotaService.deductQuotaWithFunnel(
                    virtualKey.getId(),
                    virtualKey.getUserId(),
                    virtualKey.getTeamId(),
                    virtualKey.getProjectId(),
                    weightedAmount,
                    null,  // callLogId 在日志保存后可异步回填，MVP暂传null
                    virtualKey.getQuotaType());
        }

        // 更新lastUsedTime
        try {
            virtualKeyRepository.updateLastUsedTime(virtualKey.getId(), LocalDateTime.now());
        } catch (Exception e) {
            log.warn("更新虚拟Key lastUsedTime失败: {}", e.getMessage());
        }

        // 异步记录日志
        CallLog callLog = CallLog.builder()
                .traceId(traceId)
            .userId(virtualKey.getUserId())
                .virtualKeyId(virtualKey.getId())
                .channelId(dispatchResult.getChannel().getId())
                .modelId(dispatchResult.getModel().getId())
                .modelName(dispatchResult.getModel().getModelName())
                .requestType("chat")
                .requestModel(requestModel)
                .isAutoMode(0)
                .selectedModel(dispatchResult.getModel().getModelCode())
                .selectionStrategy(dispatchResult.getStrategyUsed().name())
                .promptTokens(usage != null ? usage.getPromptTokens() : 0)
                .completionTokens(usage != null ? usage.getCompletionTokens() : 0)
                .totalTokens(usage != null ? usage.getTotalTokens() : 0)
                .responseTime(responseTime)
                .status(1)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
        callLogService.recordAsync(callLog);
    }

    private void handleFailure(VirtualKey virtualKey, DispatchResult dispatchResult,
                                Exception exception, String traceId,
                                String clientIp, String userAgent, int responseTime, String requestModel) {
        CallLog callLog = CallLog.builder()
                .traceId(traceId)
            .userId(virtualKey.getUserId())
                .virtualKeyId(virtualKey.getId())
                .channelId(dispatchResult != null ? dispatchResult.getChannel().getId() : null)
                .modelId(dispatchResult != null ? dispatchResult.getModel().getId() : null)
                .modelName(dispatchResult != null ? dispatchResult.getModel().getModelName() : null)
                .requestType("chat")
                .requestModel(requestModel)
                .isAutoMode(0)
                .selectionStrategy(dispatchResult != null ? dispatchResult.getStrategyUsed().name() : null)
                .responseTime(responseTime)
                .status(0)
                .errorCode(exception instanceof BusinessException ? String.valueOf(((BusinessException) exception).getCode()) : "500")
                .errorMessage(exception != null ? exception.getMessage() : "Unknown error")
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
        callLogService.recordAsync(callLog);
    }
}
