package com.aikey.gateway.proxy;

import com.aikey.dto.gateway.ChatCompletionRequest;
import com.aikey.dto.gateway.ChatCompletionResponse;
import com.aikey.dto.gateway.UsageInfo;
import com.aikey.entity.RealKey;
import com.aikey.exception.BusinessException;
import com.aikey.util.AesEncryptUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 代理转发服务
 *
 * <p>负责解密真实Key、构建请求、转发到厂商API、解析响应。</p>
 */
@Slf4j
@Service
public class ProxyForwardService {

    private final RestTemplate restTemplate;
    private final AesEncryptUtil aesEncryptUtil;
    private final String aesSecretKey;

    public ProxyForwardService(
            @Qualifier("proxyRestTemplate") RestTemplate restTemplate,
            AesEncryptUtil aesEncryptUtil,
            @Value("${aes.secret-key}") String aesSecretKey) {
        this.restTemplate = restTemplate;
        this.aesEncryptUtil = aesEncryptUtil;
        this.aesSecretKey = aesSecretKey;
    }

    /**
     * 转发Chat Completion请求到厂商API
     *
     * @param baseUrl 渠道的baseUrl
     * @param realKey 真实Key实体（含加密的Key值）
     * @param request 请求体
     * @return 厂商返回的响应
     */
    public ChatCompletionResponse forwardChatCompletion(String baseUrl, String channelType, RealKey realKey, ChatCompletionRequest request) {
        String url = buildUrl(baseUrl, channelType, "/chat/completions");
        String decryptedApiKey = decryptRealKey(realKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(decryptedApiKey);

        HttpEntity<ChatCompletionRequest> httpEntity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    ChatCompletionResponse.class
            );

            if (response.getBody() == null) {
                throw new BusinessException(502, "Empty response from upstream provider.");
            }

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.warn("厂商API 4xx错误: url={}, status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(e.getStatusCode().value(),
                    "Upstream provider error: " + e.getResponseBodyAsString());

        } catch (HttpServerErrorException e) {
            log.warn("厂商API 5xx错误: url={}, status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            // 5xx可以触发故障转移
            throw new BusinessException(502,
                    "Upstream provider error: " + e.getStatusCode().value());

        } catch (ResourceAccessException e) {
            log.warn("厂商API连接超时: url={}, error={}", url, e.getMessage());
            throw new BusinessException(504, "Gateway timeout: upstream provider did not respond in time.");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("代理转发异常: url={}, error={}", url, e.getMessage(), e);
            throw new BusinessException(502, "Bad gateway: " + e.getMessage());
        }
    }

    /**
     * 以 SSE 方式转发流式 Chat Completion 请求
     *
     * <p>使用 Java 11 原生 HttpClient 逐行读取 SSE 响应并转发给前端 emitter。
     * 若上游最后一个 chunk 包含 usage 字段，返回解析后的 UsageInfo；否则返回 null。</p>
     *
     * @param baseUrl     渠道的 baseUrl
     * @param channelType 渠道类型
     * @param realKey     真实 Key 实体（含加密 Key 值）
     * @param request     请求体
     * @param emitter     SseEmitter，向客户端推送数据
     * @return 上游返回的 UsageInfo（可能为 null）
     */
    public UsageInfo forwardChatCompletionStream(String baseUrl, String channelType, RealKey realKey,
                                                  ChatCompletionRequest request, SseEmitter emitter) {
        String url = buildUrl(baseUrl, channelType, "/chat/completions");
        String decryptedApiKey = decryptRealKey(realKey);

        ObjectMapper mapper = new ObjectMapper();
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to serialize request: " + e.getMessage());
        }

        // 使用 Java 11 原生 HttpClient 以支持非缓冲 SSE 流（无需额外依赖）
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + decryptedApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofMinutes(5))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        AtomicReference<UsageInfo> usageRef = new AtomicReference<>();
        AtomicBoolean emitterCompleted = new AtomicBoolean(false);

        try {
            java.net.http.HttpResponse<java.util.stream.Stream<String>> response =
                    httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                String errorBody = response.body()
                        .collect(java.util.stream.Collectors.joining("\n"));
                log.warn("厂商流式API错误: url={}, status={}, body={}", url, response.statusCode(), errorBody);
                throw new BusinessException(
                        response.statusCode() >= 500 ? 502 : response.statusCode(),
                        "Upstream provider error: " + errorBody);
            }

            response.body().forEach(line -> {
                if (!line.startsWith("data: ")) {
                    return; // 跳过空行和非 data 行
                }
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) {
                    try {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (IOException | IllegalStateException ignored) { }
                    emitterCompleted.set(true);
                    return;
                }
                if (emitterCompleted.get()) {
                    return; // 已完成，忽略后续行
                }
                // 尝试从最后一个 chunk 提取 usage
                try {
                    JsonNode node = mapper.readTree(data);
                    JsonNode usageNode = node.get("usage");
                    if (usageNode != null && !usageNode.isNull()) {
                        usageRef.set(UsageInfo.builder()
                                .promptTokens(usageNode.path("prompt_tokens").asInt(0))
                                .completionTokens(usageNode.path("completion_tokens").asInt(0))
                                .totalTokens(usageNode.path("total_tokens").asInt(0))
                                .build());
                    }
                } catch (Exception ignored) { }
                try {
                    emitter.send(SseEmitter.event().data(data));
                } catch (IOException | IllegalStateException e) {
                    log.warn("SSE客户端连接已断开或Emitter已完成: {}", e.getMessage());
                    emitterCompleted.set(true); // 停止继续推送
                }
            });

            if (!emitterCompleted.get()) {
                emitter.complete();
            }

        } catch (BusinessException e) {
            throw e;
        } catch (IllegalStateException e) {
            // emitter 已被外部（超时/客户端断连）关闭，流式提前结束，属正常情况
            log.warn("SSE Emitter 已外部关闭，流式转发提前终止: {}", e.getMessage());
            return usageRef.get();
        } catch (Exception e) {
            log.error("流式转发异常: url={}, error={}", url, e.getMessage(), e);
            try { emitter.completeWithError(e); } catch (Exception ignored) { }
            throw new BusinessException(502, "Bad gateway (stream): " + e.getMessage());
        }

        return usageRef.get();
    }

    /**
     * 解密真实Key
     */
    private String decryptRealKey(RealKey realKey) {
        try {
            return aesEncryptUtil.decrypt(realKey.getKeyValueEncrypted(), aesSecretKey);
        } catch (Exception e) {
            log.error("真实Key解密失败: realKeyId={}", realKey.getId(), e);
            throw new BusinessException(500, "Failed to decrypt real API key.");
        }
    }

    /**
     * 构建转发URL
     *
     * <p>处理baseUrl末尾的/和是否已包含/v1的情况：
     * <ul>
     *   <li>https://api.openai.com/v1 → https://api.openai.com/v1/chat/completions</li>
     *   <li>https://api.openai.com/v1/ → https://api.openai.com/v1/chat/completions</li>
     *   <li>https://api.openai.com → https://api.openai.com/v1/chat/completions</li>
     *   <li>https://open.bigmodel.cn/api/paas/v4 → https://open.bigmodel.cn/api/paas/v4/chat/completions</li>
     * </ul>
     * </p>
     */
    private String buildUrl(String baseUrl, String channelType, String path) {
        String normalizedBase = normalizeBaseUrl(baseUrl);

        if (usesDirectChatCompletionPath(channelType, normalizedBase)) {
            return normalizedBase + path;
        }

        // 如果baseUrl已经包含/v1，直接拼接path
        if (normalizedBase.endsWith("/v1")) {
            return normalizedBase + path;
        }

        // 否则拼接/v1/path
        return normalizedBase + "/v1" + path;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(500, "Channel base URL is empty.");
        }

        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    private boolean usesDirectChatCompletionPath(String channelType, String normalizedBase) {
        return "zhipu".equalsIgnoreCase(channelType)
                || normalizedBase.contains("/api/paas/v4");
    }
}
