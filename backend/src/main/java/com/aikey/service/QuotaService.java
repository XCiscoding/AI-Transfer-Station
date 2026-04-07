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
import java.math.RoundingMode;

/**
 * 额度服务
 *
 * <p>提供额度预检查和原子扣减功能。
 * 支持三层漏斗（团队→项目→虚拟Key）+ 加权扣减（模型×团队×项目倍率）。
 * 扣减使用原生SQL的WHERE条件保证并发安全，不依赖分布式锁。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final VirtualKeyRepository virtualKeyRepository;
    private final EntityManager entityManager;

    /**
     * 检查额度是否充足（原有逻辑，仅检查虚拟Key层）
     *
     * @param virtualKey 虚拟Key
     */
    public void checkQuota(VirtualKey virtualKey) {
        if (virtualKey.getQuotaRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(403, "Quota exceeded. Your API key has no remaining quota.");
        }
    }

    /**
     * 三层漏斗额度检查：min(团队剩余, 项目剩余, Key剩余)
     *
     * <p>如果虚拟Key未绑定团队或项目，则跳过对应层检查。</p>
     *
     * @param virtualKey 虚拟Key
     */
    public void checkQuotaWithFunnel(VirtualKey virtualKey) {
        // 1. 虚拟Key层检查
        if (virtualKey.getQuotaRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(403, "Quota exceeded. Your API key has no remaining quota.");
        }

        // 2. 团队层检查
        if (virtualKey.getTeamId() != null) {
            BigDecimal teamRemaining = getTeamQuotaRemaining(virtualKey.getTeamId());
            if (teamRemaining != null && teamRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(403, "Quota exceeded. Your team has no remaining quota.");
            }
        }

        // 3. 项目层检查
        if (virtualKey.getProjectId() != null) {
            BigDecimal projectRemaining = getProjectQuotaRemaining(virtualKey.getProjectId());
            if (projectRemaining != null && projectRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(403, "Quota exceeded. Your project has no remaining quota.");
            }
        }
    }

    /**
     * 计算加权消耗量
     *
     * <p>加权消耗 = token数 × 模型倍率 × 团队倍率 × 项目倍率</p>
     *
     * @param rawTokens   原始token数
     * @param modelWeight 模型倍率
     * @param teamWeight  团队倍率（null时为1.0）
     * @param projectWeight 项目倍率（null时为1.0）
     * @return 加权后的消耗量
     */
    public BigDecimal calculateWeightedAmount(BigDecimal rawTokens,
                                               BigDecimal modelWeight,
                                               BigDecimal teamWeight,
                                               BigDecimal projectWeight) {
        BigDecimal result = rawTokens;
        if (modelWeight != null) {
            result = result.multiply(modelWeight);
        }
        if (teamWeight != null) {
            result = result.multiply(teamWeight);
        }
        if (projectWeight != null) {
            result = result.multiply(projectWeight);
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 三层加权扣减
     *
     * <p>同时扣减虚拟Key、团队、项目三层额度（如果绑定了的话）。</p>
     *
     * @param virtualKeyId 虚拟Key ID
     * @param teamId       团队ID（可为null）
     * @param projectId    项目ID（可为null）
     * @param weightedAmount 加权后的扣减量
     * @return true=全部扣减成功
     */
    @Transactional
    public boolean deductQuotaWithFunnel(Long virtualKeyId, Long teamId, Long projectId, BigDecimal weightedAmount) {
        if (weightedAmount == null || weightedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        // 1. 扣减虚拟Key层
        boolean keySuccess = deductQuota(virtualKeyId, weightedAmount);
        if (!keySuccess) {
            log.warn("虚拟Key额度扣减失败: virtualKeyId={}, amount={}", virtualKeyId, weightedAmount);
            return false;
        }

        // 2. 扣减团队层
        if (teamId != null) {
            boolean teamSuccess = deductTeamQuota(teamId, weightedAmount);
            if (!teamSuccess) {
                log.warn("团队额度扣减失败: teamId={}, amount={}", teamId, weightedAmount);
                // 团队扣减失败不回滚虚拟Key扣减（允许超卖，下次预检会拦截）
            }
        }

        // 3. 扣减项目层
        if (projectId != null) {
            boolean projectSuccess = deductProjectQuota(projectId, weightedAmount);
            if (!projectSuccess) {
                log.warn("项目额度扣减失败: projectId={}, amount={}", projectId, weightedAmount);
            }
        }

        return true;
    }

    /**
     * 原子扣减虚拟Key额度（原有逻辑，保持不变）
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

    /**
     * 原子扣减团队额度
     */
    @Transactional
    public boolean deductTeamQuota(Long teamId, BigDecimal amount) {
        String sql = "UPDATE teams SET quota_used = quota_used + :amount, " +
                     "quota_remaining = quota_remaining - :amount, " +
                     "updated_at = NOW() " +
                     "WHERE id = :id AND quota_remaining >= :amount AND deleted = 0";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("amount", amount);
        query.setParameter("id", teamId);

        return query.executeUpdate() > 0;
    }

    /**
     * 原子扣减项目额度
     */
    @Transactional
    public boolean deductProjectQuota(Long projectId, BigDecimal amount) {
        String sql = "UPDATE projects SET quota_used = quota_used + :amount, " +
                     "quota_remaining = quota_remaining - :amount, " +
                     "updated_at = NOW() " +
                     "WHERE id = :id AND quota_remaining >= :amount AND deleted = 0";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("amount", amount);
        query.setParameter("id", projectId);

        return query.executeUpdate() > 0;
    }

    /**
     * 获取团队剩余额度
     */
    private BigDecimal getTeamQuotaRemaining(Long teamId) {
        try {
            String sql = "SELECT quota_remaining FROM teams WHERE id = :id AND deleted = 0";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("id", teamId);
            Object result = query.getSingleResult();
            return result != null ? new BigDecimal(result.toString()) : null;
        } catch (Exception e) {
            log.warn("查询团队额度失败: teamId={}", teamId);
            return null;
        }
    }

    /**
     * 获取项目剩余额度
     */
    private BigDecimal getProjectQuotaRemaining(Long projectId) {
        try {
            String sql = "SELECT quota_remaining FROM projects WHERE id = :id AND deleted = 0";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("id", projectId);
            Object result = query.getSingleResult();
            return result != null ? new BigDecimal(result.toString()) : null;
        } catch (Exception e) {
            log.warn("查询项目额度失败: projectId={}", projectId);
            return null;
        }
    }

    /**
     * 获取团队倍率
     */
    public BigDecimal getTeamWeight(Long teamId) {
        if (teamId == null) return BigDecimal.ONE;
        try {
            String sql = "SELECT quota_weight FROM teams WHERE id = :id AND deleted = 0";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("id", teamId);
            Object result = query.getSingleResult();
            return result != null ? new BigDecimal(result.toString()) : BigDecimal.ONE;
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    /**
     * 获取项目倍率
     */
    public BigDecimal getProjectWeight(Long projectId) {
        if (projectId == null) return BigDecimal.ONE;
        try {
            String sql = "SELECT quota_weight FROM projects WHERE id = :id AND deleted = 0";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("id", projectId);
            Object result = query.getSingleResult();
            return result != null ? new BigDecimal(result.toString()) : BigDecimal.ONE;
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }
}
