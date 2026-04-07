package com.aikey.gateway.proxy;

import com.aikey.dto.gateway.ChatCompletionRequest;
import com.aikey.dto.gateway.ChatCompletionResponse;
import com.aikey.entity.RealKey;
import com.aikey.exception.BusinessException;
import com.aikey.util.AesEncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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
    public ChatCompletionResponse forwardChatCompletion(String baseUrl, RealKey realKey, ChatCompletionRequest request) {
        String url = buildUrl(baseUrl, "/chat/completions");
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
     * </ul>
     * </p>
     */
    private String buildUrl(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        // 如果baseUrl已经包含/v1，直接拼接path
        if (normalizedBase.endsWith("/v1")) {
            return normalizedBase + path;
        }

        // 否则拼接/v1/path
        return normalizedBase + "/v1" + path;
    }
}
