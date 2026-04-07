package com.aikey.gateway.auth;

import com.aikey.dto.gateway.OpenAiErrorResponse;
import com.aikey.entity.VirtualKey;
import com.aikey.repository.VirtualKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 虚拟Key鉴权过滤器
 *
 * <p>从Authorization头提取sk-xxx格式的虚拟Key，
 * 验证Key的存在性、状态、过期时间，并将VirtualKey存入ThreadLocal上下文。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VirtualKeyAuthFilter extends OncePerRequestFilter {

    private final VirtualKeyRepository virtualKeyRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String keyValue = extractVirtualKey(request);

            if (!StringUtils.hasText(keyValue)) {
                sendError(response, 401, "Missing API key. Include it in the Authorization header: 'Bearer sk-xxx'.", "invalid_request_error", "missing_api_key");
                return;
            }

            // 查找虚拟Key
            VirtualKey virtualKey = virtualKeyRepository.findByKeyValue(keyValue).orElse(null);

            if (virtualKey == null || virtualKey.getDeleted() == 1) {
                sendError(response, 401, "Invalid API key provided.", "invalid_request_error", "invalid_api_key");
                return;
            }

            // 检查状态
            if (virtualKey.getStatus() != 1) {
                sendError(response, 403, "This API key has been disabled.", "invalid_request_error", "key_disabled");
                return;
            }

            // 检查过期时间
            if (virtualKey.getExpireTime() != null && virtualKey.getExpireTime().isBefore(LocalDateTime.now())) {
                sendError(response, 403, "This API key has expired.", "invalid_request_error", "key_expired");
                return;
            }

            // 认证通过，设置上下文
            VirtualKeyAuthContext.set(virtualKey);
            log.debug("虚拟Key认证通过: keyName={}, userId={}", virtualKey.getKeyName(), virtualKey.getUser().getId());

            filterChain.doFilter(request, response);
        } finally {
            VirtualKeyAuthContext.clear();
        }
    }

    /**
     * 从请求头提取虚拟Key值
     * 支持 "Bearer sk-xxx" 和 "sk-xxx" 两种格式
     */
    private String extractVirtualKey(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        // 兼容直接传入sk-xxx的情况
        if (authorization.startsWith("sk-")) {
            return authorization.trim();
        }
        return null;
    }

    private void sendError(HttpServletResponse response, int httpStatus, String message, String type, String code) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        OpenAiErrorResponse error = OpenAiErrorResponse.of(message, type, code);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
