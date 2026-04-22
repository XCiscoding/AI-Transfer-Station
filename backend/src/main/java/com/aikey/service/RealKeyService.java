package com.aikey.service;

import com.aikey.dto.realkey.RealKeyCreateRequest;
import com.aikey.dto.realkey.RealKeyUpdateRequest;
import com.aikey.dto.realkey.RealKeyVO;
import com.aikey.dto.common.PageResult;
import com.aikey.entity.Channel;
import com.aikey.entity.RealKey;
import com.aikey.exception.BusinessException;
import com.aikey.repository.ChannelRepository;
import com.aikey.repository.RealKeyRepository;
import com.aikey.util.AesEncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 真实Key管理服务类
 *
 * <p>提供真实API Key的增删改查、状态切换等业务逻辑</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RealKeyService {

    private final RealKeyRepository realKeyRepository;

    private final ChannelRepository channelRepository;

    private final AesEncryptUtil aesEncryptUtil;

    @Value("${aes.secret-key}")
    private String aesSecretKey;

    /**
     * 录入真实Key
     *
     * <p>流程：验证渠道存在 → AES加密 → 生成掩码 → 保存实体</p>
     *
     * @param request 录入请求（包含明文Key值）
     * @return 真实Key VO
     */
    public RealKeyVO createRealKey(RealKeyCreateRequest request) {
        log.info("录入真实Key: name={}, channelId={}", request.getKeyName(), request.getChannelId());

        // 验证渠道存在
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new BusinessException("渠道不存在"));

        try {
            // AES加密Key值
            String encryptedKeyValue = aesEncryptUtil.encrypt(request.getKeyValue(), aesSecretKey);

            // 生成掩码
            String mask = generateMask(request.getKeyValue());

            // 构建实体并保存
            RealKey realKey = RealKey.builder()
                    .keyName(request.getKeyName())
                    .keyValueEncrypted(encryptedKeyValue)
                    .keyMask(mask)
                    .channel(channel)
                    .status(1)                          // 默认启用
                    .expireTime(request.getExpireTime())
                    .usageCount(0L)
                    .remark(request.getRemark())
                    .baseUrl(request.getBaseUrl())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .deleted(0)
                    .build();

            RealKey savedRealKey = realKeyRepository.save(realKey);

            log.info("真实Key录入成功: id={}, name={}", savedRealKey.getId(), savedRealKey.getKeyName());
            return convertToVO(savedRealKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("录入真实Key失败: {}", e.getMessage(), e);
            throw new BusinessException("录入真实Key失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询真实Key详情
     *
     * @param id Key ID
     * @return 真实Key VO
     */
    public RealKeyVO getRealKeyById(Long id) {
        log.debug("查询真实Key: id={}", id);

        RealKey realKey = realKeyRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("真实Key不存在"));

        return convertToVO(realKey);
    }

    /**
     * 分页查询真实Key列表
     *
     * <p>支持按渠道ID和关键词筛选，使用JPA Specification动态查询</p>
     *
     * @param page      页码（从1开始）
     * @param size      每页大小
     * @param channelId 渠道ID筛选（可选）
     * @param keyword   关键词搜索（可选，匹配Key名称）
     * @return 分页结果
     */
    public PageResult<RealKeyVO> listRealKeys(int page, int size, Long channelId, String keyword) {
        log.debug("查询真实Key列表: page={}, size={}, channelId={}, keyword={}", page, size, channelId, keyword);

        // 构建动态查询条件
        Specification<RealKey> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();

            // 始终添加未删除条件
            predicates = cb.and(predicates, cb.equal(root.get("deleted"), 0));

            // 渠道ID精确匹配
            if (channelId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("channel").get("id"), channelId));
            }

            // 关键词搜索：Key名称模糊匹配
            if (StringUtils.hasText(keyword)) {
                predicates = cb.and(predicates, cb.like(root.get("keyName"), "%" + keyword + "%"));
            }

            return predicates;
        };

        // 分页查询（按创建时间降序）
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RealKey> pageResult = realKeyRepository.findAll(spec, pageable);

        // 转换为VO列表
        var voList = pageResult.getContent().stream()
                .map(this::convertToVO)
                .toList();

        log.debug("查询真实Key列表完成: total={}", pageResult.getTotalElements());

        return PageResult.<RealKeyVO>builder()
                .records(voList)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    /**
     * 更新真实Key信息
     *
     * @param id      Key ID
     * @param request 更新请求（只更新非空字段）
     * @return 更新后的真实Key VO
     */
    public RealKeyVO updateRealKey(Long id, RealKeyUpdateRequest request) {
        log.info("更新真实Key: id={}", id);

        // 查询真实Key
        RealKey realKey = realKeyRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("真实Key不存在"));

        try {
            // 逐字段更新非空值
            if (StringUtils.hasText(request.getKeyName())) {
                realKey.setKeyName(request.getKeyName());
            }
            if (StringUtils.hasText(request.getKeyValue())) {
                // 重新加密Key值并生成掩码
                String encryptedKeyValue = aesEncryptUtil.encrypt(request.getKeyValue(), aesSecretKey);
                realKey.setKeyValueEncrypted(encryptedKeyValue);
                realKey.setKeyMask(generateMask(request.getKeyValue()));
            }
            if (request.getExpireTime() != null) {
                realKey.setExpireTime(request.getExpireTime());
            }
            if (StringUtils.hasText(request.getRemark())) {
                realKey.setRemark(request.getRemark());
            }
            // baseUrl 允许用空字符串清除（null 表示不更新）
            if (request.getBaseUrl() != null) {
                realKey.setBaseUrl(request.getBaseUrl().isBlank() ? null : request.getBaseUrl().trim());
            }

            realKey.setUpdatedAt(LocalDateTime.now());
            RealKey updatedRealKey = realKeyRepository.save(realKey);

            log.info("真实Key更新成功: id={}", updatedRealKey.getId());
            return convertToVO(updatedRealKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新真实Key失败: id={}, error={}", id, e.getMessage(), e);
            throw new BusinessException("更新真实Key失败: " + e.getMessage());
        }
    }

    /**
     * 切换真实Key状态（启用/禁用）
     *
     * @param id Key ID
     */
    public void toggleStatus(Long id) {
        log.info("切换真实Key状态: id={}", id);

        RealKey realKey = realKeyRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("真实Key不存在"));

        // 切换状态：1->0 或 0->1
        int newStatus = realKey.getStatus() == 1 ? 0 : 1;
        realKey.setStatus(newStatus);
        realKey.setUpdatedAt(LocalDateTime.now());
        realKeyRepository.save(realKey);

        log.info("真实Key状态切换成功: id={}, newStatus={}", id, newStatus);
    }

    /**
     * 删除真实Key（逻辑删除）
     *
     * @param id Key ID
     */
    public void deleteRealKey(Long id) {
        log.info("删除真实Key: id={}", id);

        RealKey realKey = realKeyRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException("真实Key不存在"));

        realKey.setDeleted(1);
        realKey.setUpdatedAt(LocalDateTime.now());
        realKeyRepository.save(realKey);

        log.info("真实Key删除成功: id={}", id);
    }

    /**
     * 将RealKey实体转换为VO
     *
     * @param realKey 真实Key实体
     * @return 真实Key VO
     */
    private RealKeyVO convertToVO(RealKey realKey) {
        return RealKeyVO.builder()
                .id(realKey.getId())
                .keyName(realKey.getKeyName())
                .keyMask(realKey.getKeyMask())
                .channelId(realKey.getChannel().getId())
                .channelName(realKey.getChannel().getChannelName())
                .baseUrl(realKey.getBaseUrl())
                .status(realKey.getStatus())
                .expireTime(realKey.getExpireTime())
                .usageCount(realKey.getUsageCount())
                .lastUsedTime(realKey.getLastUsedTime())
                .createdAt(realKey.getCreatedAt())
                .build();
    }

    /**
     * 生成Key掩码
     *
     * <p>格式：前3位 + "***...***" + 后3位</p>
     *
     * @param keyValue 明文Key值
     * @return 掩码字符串
     */
    private String generateMask(String keyValue) {
        if (!StringUtils.hasText(keyValue)) {
            return "***";
        }

        if (keyValue.length() > 6) {
            return keyValue.substring(0, 3) + "***...***" + keyValue.substring(keyValue.length() - 3);
        } else {
            return "***";
        }
    }
}
