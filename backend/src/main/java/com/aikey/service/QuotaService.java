package com.aikey.service;

import com.aikey.entity.VirtualKey;
import com.aikey.exception.BusinessException;
import com.aikey.repository.VirtualKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;

/**
 * 额度服务
 *
 * <p>提供额度预检查和原子扣减功能。
 * 扣减使用原生SQL的WHERE条件保证并发安全，不依赖分布式锁。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final VirtualKeyRepository virtualKeyRepository;
    private final EntityManager entityManager;

    /**
     * 检查额度是否充足
     *
     * @param virtualKey 虚拟Key
     */
    public void checkQuota(VirtualKey virtualKey) {
        if (virtualKey.getQuotaRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(403, "Quota exceeded. Your API key has no remaining quota.");
        }
    }

    /**
     * 原子扣减额度
     *
     * <p>使用原生SQL UPDATE ... WHERE quota_remaining >= amount 保证并发安全。
     * 如果0行被更新，说明额度不足。</p>
     *
     * @param virtualKeyId 虚拟Key ID
     * @param amount       扣减量
     * @return true=扣减成功, false=额度不足
     */
    @Transactional
    public boolean deductQuota(Long virtualKeyId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        String sql = "UPDATE virtual_keys SET quota_used = quota_used + :amount, " +
                     "quota_remaining = quota_remaining - :amount, " +
                     "updated_at = NOW() " +
                     "WHERE id = :id AND quota_remaining >= :amount AND deleted = 0";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("amount", amount);
        query.setParameter("id", virtualKeyId);

        int updated = query.executeUpdate();
        if (updated == 0) {
            log.warn("额度扣减失败，虚拟Key ID: {}, 扣减量: {}", virtualKeyId, amount);
            return false;
        }

        log.debug("额度扣减成功，虚拟Key ID: {}, 扣减量: {}", virtualKeyId, amount);
        return true;
    }
}
