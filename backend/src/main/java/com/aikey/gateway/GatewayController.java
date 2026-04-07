package com.aikey.gateway;

import com.aikey.dto.gateway.ChatCompletionRequest;
import com.aikey.dto.gateway.ChatCompletionResponse;
import com.aikey.dto.gateway.OpenAiErrorResponse;
import com.aikey.exception.BusinessException;
import com.aikey.service.GatewayOrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 代理网关控制器
 *
 * <p>兼容OpenAI接口格式，接收所有模型调用请求并进行路由。
 * 使用虚拟Key（sk-xxx）鉴权，由VirtualKeyAuthFilter处理。</p>
 *
 * <p>注意：本控制器不使用Result包装，直接返回OpenAI兼容格式。</p>
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayOrchestrationService orchestrationService;

    /**
     * Chat Completion接口（兼容OpenAI格式）
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<ChatCompletionResponse> chatCompletion(
            @RequestBody ChatCompletionRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        ChatCompletionResponse response = orchestrationService.processChatCompletion(
                request, clientIp, userAgent);

        return ResponseEntity.ok(response);
    }

    // ==================== 异常处理（OpenAI格式） ====================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<OpenAiErrorResponse> handleBusinessException(BusinessException e) {
        String type = mapErrorType(e.getCode());
        String code = mapErrorCode(e.getCode());

        OpenAiErrorResponse error = OpenAiErrorResponse.of(e.getMessage(), type, code);

        HttpStatus status = switch (e.getCode()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 502 -> HttpStatus.BAD_GATEWAY;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 504 -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OpenAiErrorResponse> handleGenericException(Exception e) {
        log.error("网关未捕获异常", e);
        OpenAiErrorResponse error = OpenAiErrorResponse.of(
                "Internal server error", "server_error", "internal_error");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== 辅助方法 ====================

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String mapErrorType(int code) {
        return switch (code) {
            case 400 -> "invalid_request_error";
            case 401 -> "invalid_request_error";
            case 403 -> "invalid_request_error";
            case 404 -> "invalid_request_error";
            case 429 -> "rate_limit_error";
            case 502, 503, 504 -> "server_error";
            default -> "server_error";
        };
    }

    private String mapErrorCode(int code) {
        return switch (code) {
            case 400 -> "invalid_request";
            case 401 -> "invalid_api_key";
            case 403 -> "insufficient_quota";
            case 404 -> "model_not_found";
            case 429 -> "rate_limit_exceeded";
            case 502 -> "bad_gateway";
            case 503 -> "service_unavailable";
            case 504 -> "gateway_timeout";
            default -> "internal_error";
        };
    }
}
