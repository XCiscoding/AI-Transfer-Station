package com.aikey.service;

import com.aikey.dto.channel.ChannelCreateRequest;
import com.aikey.dto.channel.ChannelUpdateRequest;
import com.aikey.dto.channel.ChannelVO;
import com.aikey.entity.Channel;
import com.aikey.exception.BusinessException;
import com.aikey.repository.ChannelRepository;
import com.aikey.util.AesEncryptUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 渠道管理服务类
 *
 * <p>提供渠道的增删改查、状态切换、连通性测试等业务逻辑</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;

    private final AesEncryptUtil aesEncryptUtil;

    private final ObjectMapper objectMapper;

    @Value("${aes.secret-key}")
    private String aesSecretKey;

    /**
     * 创建渠道
     *
     * @param request 创建请求
     * @return 渠道VO
     */
    public ChannelVO createChannel(ChannelCreateRequest request) {
        log.info("创建渠道: {}", request.getChannelName());

        // 检查渠道编码唯一性
        if (channelRepository.findByChannelCode(request.getChannelCode()).isPresent()) {
            throw new BusinessException("渠道编码已存在");
        }

        try {
            // AES加密API Key
            String encryptedApiKey = aesEncryptUtil.encrypt(request.getApiKey(), aesSecretKey);

            // 构建扩展配置 JSON
            String configJson = buildConfigJson(
                    request.getProvider(), request.getApiVersion(),
                    request.getMaxTokens(), request.getMaxRpm(),
                    request.getMaxTpm(), request.getTimeout());

            // 构建实体并保存
            Channel channel = Channel.builder()
                    .channelName(request.getChannelName())
                    .channelCode(request.getChannelCode())
                    .channelType(request.getChannelType())
                    .baseUrl(request.getBaseUrl())
                    .apiKeyEncrypted(encryptedApiKey)
                    .weight(request.getWeight())
                    .priority(request.getPriority())
                    .remark(request.getRemark())
                    .config(configJson)
                    .status(1)
                    .healthStatus(1)
                    .successCount(0L)
                    .failCount(0L)
                    .avgResponseTime(0)
                    .deleted(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Channel savedChannel = channelRepository.save(channel);

            log.info("渠道创建成功: id={}, name={}", savedChannel.getId(), savedChannel.getChannelName());
            return convertToVO(savedChannel);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建渠道失败: {}", e.getMessage(), e);
            throw new BusinessException("创建渠道失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询渠道列表
     *
     * @param page        页码（从1开始）
     * @param size        每页大小
     * @param keyword     关键词（可选）
     * @param channelType 渠道类型（可选）
     * @return 分页结果
     */
    public com.aikey.dto.common.PageResult<ChannelVO> listChannels(int page, int size, String keyword, String channelType) {
        log.debug("查询渠道列表: page={}, size={}, keyword={}, type={}", page, size, keyword, channelType);

        // 构建动态查询条件
        Specification<Channel> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();

            // 始终添加未删除条件
            predicates = cb.and(predicates, cb.equal(root.get("deleted"), 0));

            // 关键词搜索：渠道名称或渠道编码模糊匹配
            if (StringUtils.hasText(keyword)) {
                predicates = cb.and(predicates, cb.or(
                        cb.like(root.get("channelName"), "%" + keyword + "%"),
                        cb.like(root.get("channelCode"), "%" + keyword + "%")
                ));
            }

            // 渠道类型精确匹配
            if (StringUtils.hasText(channelType)) {
                predicates = cb.and(predicates, cb.equal(root.get("channelType"), channelType));
            }

            return predicates;
        };

        // 分页查询（按创建时间降序）
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Channel> pageResult = channelRepository.findAll(spec, pageable);

        // 转换为VO列表
        java.util.List<ChannelVO> voList = pageResult.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        log.debug("查询渠道列表完成: total={}", pageResult.getTotalElements());

        return com.aikey.dto.common.PageResult.<ChannelVO>builder()
                .records(voList)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    /**
     * 更新渠道信息
     *
     * @param id      渠道ID
     * @param request 更新请求
     * @return 更新后的渠道VO
     */
    public ChannelVO updateChannel(Long id, ChannelUpdateRequest request) {
        log.info("更新渠道: id={}", id);

        // 查询渠道
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("渠道不存在"));

        try {
            // 逐字段更新非空值
            if (StringUtils.hasText(request.getChannelName())) {
                channel.setChannelName(request.getChannelName());
            }
            if (StringUtils.hasText(request.getBaseUrl())) {
                channel.setBaseUrl(request.getBaseUrl());
            }
            if (StringUtils.hasText(request.getApiKey())) {
                // 重新加密API Key
                String encryptedApiKey = aesEncryptUtil.encrypt(request.getApiKey(), aesSecretKey);
                channel.setApiKeyEncrypted(encryptedApiKey);
            }
            if (request.getWeight() != null) {
                channel.setWeight(request.getWeight());
            }
            if (request.getPriority() != null) {
                channel.setPriority(request.getPriority());
            }
            // remark 允许清空：null 表示不更新，空字符串/非空都执行更新
            if (request.getRemark() != null) {
                channel.setRemark(request.getRemark());
            }
            if (request.getStatus() != null) {
                channel.setStatus(request.getStatus());
            }

            // 更新扩展配置字段（写入 config JSON）
            boolean hasConfigUpdate = request.getProvider() != null
                    || request.getApiVersion() != null
                    || request.getMaxTokens() != null
                    || request.getMaxRpm() != null
                    || request.getMaxTpm() != null
                    || request.getTimeout() != null;
            if (hasConfigUpdate) {
                Map<String, Object> cfg = parseConfigJson(channel.getConfig());
                if (request.getProvider() != null)   cfg.put("provider",   request.getProvider());
                if (request.getApiVersion() != null) cfg.put("apiVersion", request.getApiVersion());
                if (request.getMaxTokens() != null)  cfg.put("maxTokens",  request.getMaxTokens());
                if (request.getMaxRpm() != null)     cfg.put("maxRpm",     request.getMaxRpm());
                if (request.getMaxTpm() != null)     cfg.put("maxTpm",     request.getMaxTpm());
                if (request.getTimeout() != null)    cfg.put("timeout",    request.getTimeout());
                channel.setConfig(objectMapper.writeValueAsString(cfg));
            }

            channel.setUpdatedAt(LocalDateTime.now());
            Channel updatedChannel = channelRepository.save(channel);

            log.info("渠道更新成功: id={}", updatedChannel.getId());
            return convertToVO(updatedChannel);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新渠道失败: id={}, error={}", id, e.getMessage(), e);
            throw new BusinessException("更新渠道失败: " + e.getMessage());
        }
    }

    /**
     * 删除渠道（逻辑删除）
     *
     * @param id 渠道ID
     */
    public void deleteChannel(Long id) {
        log.info("删除渠道: id={}", id);

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("渠道不存在"));

        channel.setDeleted(1);
        channel.setUpdatedAt(LocalDateTime.now());
        channelRepository.save(channel);

        log.info("渠道删除成功: id={}", id);
    }

    /**
     * 切换渠道状态（启用/禁用）
     *
     * @param id 渠道ID
     */
    public void toggleStatus(Long id) {
        log.info("切换渠道状态: id={}", id);

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("渠道不存在"));

        // 切换状态：1->0 或 0->1
        int newStatus = channel.getStatus() == 1 ? 0 : 1;
        channel.setStatus(newStatus);
        channel.setUpdatedAt(LocalDateTime.now());
        channelRepository.save(channel);

        log.info("渠道状态切换成功: id={}, newStatus={}", id, newStatus);
    }

    /**
     * 测试渠道连通性（异步执行）
     *
     * <p>通过调用渠道的 /models 接口测试API连接是否可用</p>
     *
     * @param id 渠道ID
     * @return 连通性测试结果（异步）
     */
    @Async
    public CompletableFuture<Boolean> testConnection(Long id) {
        log.info("测试渠道连通性: id={}", id);

        boolean success = false;

        try {
            Channel channel = channelRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("渠道不存在"));

            // 解密API Key
            String apiKey = aesEncryptUtil.decrypt(channel.getApiKeyEncrypted(), aesSecretKey);

            // 构建HTTP请求
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 发送GET请求到 /models 端点
            String url = channel.getBaseUrl().endsWith("/")
                    ? channel.getBaseUrl() + "models"
                    : channel.getBaseUrl() + "/models";

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            success = response.getStatusCode().is2xxSuccessful();

            // 更新健康状态
            if (success) {
                channel.setHealthStatus(1);
                channel.setSuccessCount(channel.getSuccessCount() + 1);
            } else {
                channel.setHealthStatus(0);
                channel.setFailCount(channel.getFailCount() + 1);
            }

            channel.setHealthCheckTime(LocalDateTime.now());
            channel.setUpdatedAt(LocalDateTime.now());
            channelRepository.save(channel);

            log.info("渠道连通性测试完成: id={}, success={}", id, success);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("渠道连通性测试失败: id={}, error={}", id, e.getMessage(), e);

            // 更新健康状态为不健康
            try {
                Channel channel = channelRepository.findById(id).orElse(null);
                if (channel != null) {
                    channel.setHealthStatus(0);
                    channel.setFailCount(channel.getFailCount() + 1);
                    channel.setHealthCheckTime(LocalDateTime.now());
                    channel.setUpdatedAt(LocalDateTime.now());
                    channelRepository.save(channel);
                }
            } catch (Exception ex) {
                log.error("更新健康状态失败: id={}", id, ex);
            }
        }

        return CompletableFuture.completedFuture(success);
    }

    /**
     * 将Channel实体转换为VO
     *
     * @param channel 渠道实体
     * @return 渠道VO
     */
    private ChannelVO convertToVO(Channel channel) {
        Map<String, Object> cfg = parseConfigJson(channel.getConfig());

        return ChannelVO.builder()
                .id(channel.getId())
                .channelName(channel.getChannelName())
                .channelCode(channel.getChannelCode())
                .channelType(channel.getChannelType())
                .baseUrl(channel.getBaseUrl())
                .apiKeyMask(maskApiKey(channel.getApiKeyEncrypted()))
                .weight(channel.getWeight())
                .priority(channel.getPriority())
                .status(channel.getStatus())
                .healthStatus(channel.getHealthStatus())
                .healthCheckTime(channel.getHealthCheckTime())
                .createdAt(channel.getCreatedAt())
                .provider((String) cfg.get("provider"))
                .apiVersion((String) cfg.get("apiVersion"))
                .maxTokens(cfg.get("maxTokens") != null ? ((Number) cfg.get("maxTokens")).intValue() : null)
                .maxRpm(cfg.get("maxRpm") != null ? ((Number) cfg.get("maxRpm")).intValue() : null)
                .maxTpm(cfg.get("maxTpm") != null ? ((Number) cfg.get("maxTpm")).intValue() : null)
                .timeout(cfg.get("timeout") != null ? ((Number) cfg.get("timeout")).intValue() : null)
                .build();
    }

    /**
     * 对加密的API Key进行掩码处理
     *
     * <p>格式：前3位 + "***...***" + 后3位</p>
     *
     * @param encryptedKey 加密的API Key
     * @return 掩码后的字符串
     */
    private String buildConfigJson(String provider, String apiVersion,
                                   Integer maxTokens, Integer maxRpm,
                                   Integer maxTpm, Integer timeout) {
        try {
            Map<String, Object> cfg = new HashMap<>();
            if (provider != null)   cfg.put("provider",   provider);
            if (apiVersion != null) cfg.put("apiVersion", apiVersion);
            if (maxTokens != null)  cfg.put("maxTokens",  maxTokens);
            if (maxRpm != null)     cfg.put("maxRpm",     maxRpm);
            if (maxTpm != null)     cfg.put("maxTpm",     maxTpm);
            if (timeout != null)    cfg.put("timeout",    timeout);
            return cfg.isEmpty() ? null : objectMapper.writeValueAsString(cfg);
        } catch (Exception e) {
            log.warn("构建 config JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseConfigJson(String configJson) {
        try {
            if (!StringUtils.hasText(configJson)) return new HashMap<>();
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析 config JSON 失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String maskApiKey(String encryptedKey) {
        try {
            if (!StringUtils.hasText(encryptedKey)) {
                return "***";
            }

            String decryptedKey = aesEncryptUtil.decrypt(encryptedKey, aesSecretKey);

            if (decryptedKey.length() > 10) {
                return decryptedKey.substring(0, 3) + "***...***" + decryptedKey.substring(decryptedKey.length() - 3);
            } else {
                return "***";
            }
        } catch (Exception e) {
            log.warn("API Key掩码处理失败: {}", e.getMessage());
            return "***";
        }
    }
}
