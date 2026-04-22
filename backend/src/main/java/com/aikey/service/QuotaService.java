package com.aikey.service;

import com.aikey.dto.common.PageResult;
import com.aikey.dto.quota.QuotaSummaryVO;
import com.aikey.dto.quota.QuotaTransactionVO;
import com.aikey.entity.QuotaTransaction;
import com.aikey.exception.BusinessException;
import com.aikey.repository.QuotaTransactionRepository;
import com.aikey.repository.VirtualKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 额度服务
 *
 * <p>提供额度预检查、原子扣减、充值、调整、重置功能，并记录完整的额度流水。
 * 支持三层漏斗（虚拟Key → 团队 → 项目）+ 加权扣减（模型×团队×项目倍率）。
 * 扣减使用原生SQL的 WHERE quota_remaining >= :amount 条件保证并发安全。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final VirtualKeyRepository virtualKeyRepository;
    private final QuotaTransactionRepository transactionRepository;
    private final EntityManager entityManager;

    // ===== 额度预检查 =====

    /**
     * 检查额度是否充足（仅虚拟Key层，旧兼容接口）
     */
    public void checkQuota(com.aikey.entity.VirtualKey virtualKey) {
        if (virtualKey.getQuotaRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(403, "Quota exceeded. Your API key has no remaining quota.");
        }
    }

    /**
     * 三层漏斗额度检查：Key层 → 团队层 → 项目层
     *
     * <p>任意一层余量为0则抛出403异常，未绑定的层跳过检查。</p>
     */
    public void checkQuotaWithFunnel(com.aikey.entity.VirtualKey virtualKey) {
        if (virtualKey.getQuotaRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(403, "Quota exceeded. Your API key has no remaining quota.");
        }

        if (virtualKey.getTeamId() != null) {
            BigDecimal teamRemaining = getTeamQuotaRemaining(virtualKey.getTeamId());
            if (teamRemaining != null && teamRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(403, "Quota exceeded. Your team has no remaining quota.");
            }
        }

        if (virtualKey.getProjectId() != null) {
            BigDecimal projectRemaining = getProjectQuotaRemaining(virtualKey.getProjectId());
            if (projectRemaining != null && projectRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(403, "Quota exceeded. Your project has no remaining quota.");
            }
        }
    }

    // ===== 加权计算 =====

    /**
     * 计算加权消耗量：rawTokens × 模型倍率 × 团队倍率 × 项目倍率
     */
    public BigDecimal calculateWeightedAmount(BigDecimal rawTokens,
                                               BigDecimal modelWeight,
                                               BigDecimal teamWeight,
                                               BigDecimal projectWeight) {
        BigDecimal result = rawTokens;
        if (modelWeight != null) result = result.multiply(modelWeight);
        if (teamWeight != null)  result = result.multiply(teamWeight);
        if (projectWeight != null) result = result.multiply(projectWeight);
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    // ===== 三层扣减（调用消耗） =====

    /**
     * 三层加权扣减（调用成功后异步执行），同时写流水记录
     *
     * @param virtualKeyId   虚拟Key ID
     * @param userId         操作用户ID（用于流水归属）
     * @param teamId         团队ID（null则跳过）
     * @param projectId      项目ID（null则跳过）
     * @param weightedAmount 加权扣减量
     * @param callLogId      关联调用日志ID
     * @param quotaType      额度类型（token/count/amount）
     * @return true=Key层扣减成功
     */
    @Transactional
    public boolean deductQuotaWithFunnel(Long virtualKeyId, Long userId, Long teamId, Long projectId,
                                          BigDecimal weightedAmount, Long callLogId, String quotaType) {
        if (weightedAmount == null || weightedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }

        // 1. 扣减虚拟Key层
        BigDecimal keyBefore = getVirtualKeyQuotaRemaining(virtualKeyId);
        boolean keySuccess = deductVirtualKeyQuota(virtualKeyId, weightedAmount);
        if (!keySuccess) {
            log.warn("虚拟Key额度扣减失败: virtualKeyId={}, amount={}", virtualKeyId, weightedAmount);
            return false;
        }
        recordTransaction(userId, virtualKeyId, callLogId, quotaType,
                "consume", "virtual_key", virtualKeyId,
                weightedAmount.negate(), keyBefore, keyBefore.subtract(weightedAmount),
                "API调用消耗");

        // 2. 扣减团队层
        if (teamId != null) {
            BigDecimal teamBefore = getTeamQuotaRemaining(teamId);
            boolean teamSuccess = deductTeamQuota(teamId, weightedAmount);
            if (!teamSuccess) {
                log.warn("团队额度扣减失败（不回滚Key层）: teamId={}, amount={}", teamId, weightedAmount);
            } else if (teamBefore != null) {
                recordTransaction(userId, virtualKeyId, callLogId, quotaType,
                        "consume", "team", teamId,
                        weightedAmount.negate(), teamBefore, teamBefore.subtract(weightedAmount),
                        "API调用消耗（来自虚拟Key " + virtualKeyId + "）");
            }
        }

        // 3. 扣减项目层
        if (projectId != null) {
            BigDecimal projBefore = getProjectQuotaRemaining(projectId);
            boolean projSuccess = deductProjectQuota(projectId, weightedAmount);
            if (!projSuccess) {
                log.warn("项目额度扣减失败（不回滚Key层）: projectId={}, amount={}", projectId, weightedAmount);
            } else if (projBefore != null) {
                recordTransaction(userId, virtualKeyId, callLogId, quotaType,
                        "consume", "project", projectId,
                        weightedAmount.negate(), projBefore, projBefore.subtract(weightedAmount),
                        "API调用消耗（来自虚拟Key " + virtualKeyId + "）");
            }
        }

        return true;
    }

    /**
     * 兼容旧调用签名（无 userId / callLogId / quotaType）
     */
    @Transactional
    public boolean deductQuotaWithFunnel(Long virtualKeyId, Long teamId, Long projectId, BigDecimal weightedAmount) {
        return deductQuotaWithFunnel(virtualKeyId, null, teamId, projectId, weightedAmount, null, "token");
    }

    // ===== 充值 =====

    /**
     * 给虚拟Key充值额度
     */
    @Transactional
    public void rechargeVirtualKey(Long virtualKeyId, Long operatorId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }
        String sql = "UPDATE virtual_keys SET quota_limit = quota_limit + :amount, " +
                     "quota_remaining = quota_remaining + :amount, updated_at = NOW() " +
                     "WHERE id = :id AND deleted = 0";
        int rows = buildAndExecute(sql, amount, virtualKeyId);
        if (rows == 0) throw new BusinessException("虚拟Key不存在或已删除");

        BigDecimal before = getVirtualKeyQuotaRemaining(virtualKeyId).subtract(amount);
        recordTransaction(operatorId, virtualKeyId, null, "token",
                "recharge", "virtual_key", virtualKeyId,
                amount, before, before.add(amount),
                description != null ? description : "手动充值");
    }

    /**
     * 给团队充值额度
     */
    @Transactional
    public void rechargeTeam(Long teamId, Long operatorId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }
        String sql = "UPDATE teams SET quota_limit = quota_limit + :amount, " +
                     "quota_remaining = quota_remaining + :amount, updated_at = NOW() " +
                     "WHERE id = :id AND deleted = 0";
        int rows = buildAndExecute(sql, amount, teamId);
        if (rows == 0) throw new BusinessException("团队不存在或已删除");

        BigDecimal before = getTeamQuotaRemaining(teamId).subtract(amount);
        recordTransaction(operatorId, null, null, "token",
                "recharge", "team", teamId,
                amount, before, before.add(amount),
                description != null ? description : "手动充值");
    }

    /**
     * 给项目充值额度
     */
    @Transactional
    public void rechargeProject(Long projectId, Long operatorId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }
        String sql = "UPDATE projects SET quota_limit = quota_limit + :amount, " +
                     "quota_remaining = quota_remaining + :amount, updated_at = NOW() " +
                     "WHERE id = :id AND deleted = 0";
        int rows = buildAndExecute(sql, amount, projectId);
        if (rows == 0) throw new BusinessException("项目不存在或已删除");

        BigDecimal before = getProjectQuotaRemaining(projectId).subtract(amount);
        recordTransaction(operatorId, null, null, "token",
                "recharge", "project", projectId,
                amount, before, before.add(amount),
                description != null ? description : "手动充值");
    }

    // ===== 重置（清零已用，剩余=上限） =====

    /**
     * 重置虚拟Key已用额度为0（quota_remaining 恢复为 quota_limit）
     */
    @Transactional
    public void resetVirtualKeyQuota(Long virtualKeyId, Long operatorId) {
        BigDecimal before = getVirtualKeyQuotaRemaining(virtualKeyId);
        String sql = "UPDATE virtual_keys SET quota_used = 0, quota_remaining = quota_limit, updated_at = NOW() " +
                     "WHERE id = :id AND deleted = 0";
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("id", virtualKeyId);
        if (q.executeUpdate() == 0) throw new BusinessException("虚拟Key不存在或已删除");

        BigDecimal after = getVirtualKeyQuotaRemaining(virtualKeyId);
        BigDecimal delta = after.subtract(before);
        recordTransaction(operatorId, virtualKeyId, null, "token",
                "reset", "virtual_key", virtualKeyId,
                delta, before, after, "额度重置");
    }

    /**
     * 重置团队已用额度
     */
    @Transactional
    public void resetTeamQuota(Long teamId, Long operatorId) {
        BigDecimal before = getTeamQuotaRemaining(teamId);
        String sql = "UPDATE teams SET quota_used = 0, quota_remaining = quota_limit, updated_at = NOW() " +
                     "WHERE id = :id AND deleted = 0";
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("id", teamId);
        if (q.executeUpdate() == 0) throw new BusinessException("团队不存在或已删除");

        BigDecimal after = getTeamQuotaRemaining(teamId);
        recordTransaction(operatorId, null, null, "token",
                "reset", "team", teamId,
                after.subtract(before), before, after, "额度重置");
    }

    /**
     * 重置项目已用额度
     */
    @Transactional
    public void resetProjectQuota(Long projectId, Long operatorId) {
        BigDecimal before = getProjectQuotaRemaining(projectId);
        String sql = "UPDATE projects SET quota_used = 0, quota_remaining = quota_limit, updated_at = NOW() " +
                     "WHERE id = :id AND deleted = 0";
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("id", projectId);
        if (q.executeUpdate() == 0) throw new BusinessException("项目不存在或已删除");

        BigDecimal after = getProjectQuotaRemaining(projectId);
        recordTransaction(operatorId, null, null, "token",
                "reset", "project", projectId,
                after.subtract(before), before, after, "额度重置");
    }

    // ===== 手动调整（正负均可） =====

    /**
     * 手动调整虚拟Key额度（amount为正则增加，为负则减少）
     */
    @Transactional
    public void adjustVirtualKeyQuota(Long virtualKeyId, Long operatorId, BigDecimal delta, String description) {
        BigDecimal before = getVirtualKeyQuotaRemaining(virtualKeyId);
        if (delta.compareTo(BigDecimal.ZERO) >= 0) {
            String sql = "UPDATE virtual_keys SET quota_limit = quota_limit + :amount, " +
                         "quota_remaining = quota_remaining + :amount, updated_at = NOW() " +
                         "WHERE id = :id AND deleted = 0";
            if (buildAndExecute(sql, delta, virtualKeyId) == 0) throw new BusinessException("虚拟Key不存在或已删除");
        } else {
            BigDecimal absDelta = delta.abs();
            String sql = "UPDATE virtual_keys SET quota_used = quota_used + :amount, " +
                         "quota_remaining = quota_remaining - :amount, updated_at = NOW() " +
                         "WHERE id = :id AND quota_remaining >= :amount AND deleted = 0";
            if (buildAndExecute(sql, absDelta, virtualKeyId) == 0)
                throw new BusinessException("余额不足或虚拟Key不存在");
        }
        BigDecimal after = getVirtualKeyQuotaRemaining(virtualKeyId);
        recordTransaction(operatorId, virtualKeyId, null, "token",
                "adjust", "virtual_key", virtualKeyId,
                delta, before, after, description != null ? description : "手动调整");
    }

    // ===== 倍率查询 =====

    public BigDecimal getTeamWeight(Long teamId) {
        if (teamId == null) return BigDecimal.ONE;
        try {
            Query q = entityManager.createNativeQuery(
                    "SELECT quota_weight FROM teams WHERE id = :id AND deleted = 0");
            q.setParameter("id", teamId);
            Object r = q.getSingleResult();
            return r != null ? new BigDecimal(r.toString()) : BigDecimal.ONE;
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    public BigDecimal getProjectWeight(Long projectId) {
        if (projectId == null) return BigDecimal.ONE;
        try {
            Query q = entityManager.createNativeQuery(
                    "SELECT quota_weight FROM projects WHERE id = :id AND deleted = 0");
            q.setParameter("id", projectId);
            Object r = q.getSingleResult();
            return r != null ? new BigDecimal(r.toString()) : BigDecimal.ONE;
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    // ===== 单层原子扣减（内部方法） =====

    @Transactional
    public boolean deductQuota(Long virtualKeyId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return true;
        String sql = "UPDATE virtual_keys SET quota_used = quota_used + :amount, " +
                     "quota_remaining = quota_remaining - :amount, updated_at = NOW() " +
                     "WHERE id = :id AND quota_remaining >= :amount AND deleted = 0";
        int rows = buildAndExecute(sql, amount, virtualKeyId);
        if (rows == 0) {
            log.warn("额度扣减失败，Key ID: {}, 扣减量: {}", virtualKeyId, amount);
            return false;
        }
        return true;
    }

    @Transactional
    public boolean deductTeamQuota(Long teamId, BigDecimal amount) {
        String sql = "UPDATE teams SET quota_used = quota_used + :amount, " +
                     "quota_remaining = quota_remaining - :amount, updated_at = NOW() " +
                     "WHERE id = :id AND quota_remaining >= :amount AND deleted = 0";
        return buildAndExecute(sql, amount, teamId) > 0;
    }

    @Transactional
    public boolean deductProjectQuota(Long projectId, BigDecimal amount) {
        String sql = "UPDATE projects SET quota_used = quota_used + :amount, " +
                     "quota_remaining = quota_remaining - :amount, updated_at = NOW() " +
                     "WHERE id = :id AND quota_remaining >= :amount AND deleted = 0";
        return buildAndExecute(sql, amount, projectId) > 0;
    }

    private boolean deductVirtualKeyQuota(Long virtualKeyId, BigDecimal amount) {
        return deductQuota(virtualKeyId, amount);
    }

    // ===== 余额查询（内部） =====

    BigDecimal getVirtualKeyQuotaRemaining(Long virtualKeyId) {
        return queryDecimal("SELECT quota_remaining FROM virtual_keys WHERE id = :id AND deleted = 0", virtualKeyId);
    }

    BigDecimal getTeamQuotaRemaining(Long teamId) {
        return queryDecimal("SELECT quota_remaining FROM teams WHERE id = :id AND deleted = 0", teamId);
    }

    BigDecimal getProjectQuotaRemaining(Long projectId) {
        return queryDecimal("SELECT quota_remaining FROM projects WHERE id = :id AND deleted = 0", projectId);
    }

    private BigDecimal queryDecimal(String sql, Long id) {
        try {
            Query q = entityManager.createNativeQuery(sql);
            q.setParameter("id", id);
            Object r = q.getSingleResult();
            return r != null ? new BigDecimal(r.toString()) : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ===== 流水写入（异步，失败不影响主流程） =====

    @Async
    void recordTransaction(Long userId, Long virtualKeyId, Long callLogId,
                           String quotaType, String transactionType,
                           String targetType, Long targetId,
                           BigDecimal amount, BigDecimal balanceBefore, BigDecimal balanceAfter,
                           String description) {
        try {
            QuotaTransaction tx = QuotaTransaction.builder()
                    .userId(userId != null ? userId : 0L)
                    .virtualKeyId(virtualKeyId)
                    .callLogId(callLogId)
                    .quotaType(quotaType)
                    .transactionType(transactionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .amount(amount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .description(description)
                    .build();
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.warn("写入额度流水失败（不影响主流程）: {}", e.getMessage());
        }
    }

    // ===== 流水查询（分页） =====

    /**
     * 分页查询额度流水
     *
     * @param targetType 目标类型（可选）
     * @param targetId   目标ID（可选，需配合 targetType）
     * @param userId     用户ID（可选）
     * @param page       页码（从1开始）
     * @param size       每页大小
     */
    public PageResult<QuotaTransactionVO> listTransactions(
            String targetType, Long targetId, Long userId, int page, int size) {

        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<QuotaTransaction> pageResult;

        if (targetType != null && targetId != null) {
            pageResult = transactionRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                    targetType, targetId, pageable);
        } else if (userId != null) {
            pageResult = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            pageResult = transactionRepository.findAll(pageable);
        }

        List<QuotaTransactionVO> records = pageResult.getContent()
                .stream()
                .map(this::convertToTransactionVO)
                .collect(Collectors.toList());

        return PageResult.<QuotaTransactionVO>builder()
                .records(records)
                .total(pageResult.getTotalElements())
                .current((long) page)
                .size((long) size)
                .build();
    }

    private QuotaTransactionVO convertToTransactionVO(QuotaTransaction tx) {
        return QuotaTransactionVO.builder()
                .id(tx.getId())
                .userId(tx.getUserId())
                .virtualKeyId(tx.getVirtualKeyId())
                .callLogId(tx.getCallLogId())
                .quotaType(tx.getQuotaType())
                .transactionType(tx.getTransactionType())
                .targetType(tx.getTargetType())
                .targetId(tx.getTargetId())
                .amount(tx.getAmount())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    // ===== 余额摘要查询 =====

    /**
     * 查询指定目标的额度摘要
     */
    public QuotaSummaryVO getSummary(String targetType, Long targetId) {
        QuotaSummaryVO.QuotaSummaryVOBuilder builder =
                QuotaSummaryVO.builder()
                        .targetType(targetType)
                        .targetId(targetId);

        switch (targetType) {
            case "virtual_key" -> {
                Object[] row = querySummaryRow(
                        "SELECT key_name, quota_limit, quota_used, quota_remaining, quota_type " +
                        "FROM virtual_keys WHERE id = :id AND deleted = 0", targetId);
                if (row == null) throw new BusinessException("虚拟Key不存在：" + targetId);
                builder.targetName(str(row[0]))
                        .quotaLimit(dec(row[1]))
                        .quotaUsed(dec(row[2]))
                        .quotaRemaining(dec(row[3]))
                        .quotaType(str(row[4]));
            }
            case "team" -> {
                Object[] row = querySummaryRow(
                        "SELECT team_name, quota_limit, quota_used, quota_remaining, 'token' " +
                        "FROM teams WHERE id = :id AND deleted = 0", targetId);
                if (row == null) throw new BusinessException("团队不存在：" + targetId);
                builder.targetName(str(row[0]))
                        .quotaLimit(dec(row[1]))
                        .quotaUsed(dec(row[2]))
                        .quotaRemaining(dec(row[3]))
                        .quotaType(str(row[4]));
            }
            case "project" -> {
                Object[] row = querySummaryRow(
                        "SELECT project_name, quota_limit, quota_used, quota_remaining, 'token' " +
                        "FROM projects WHERE id = :id AND deleted = 0", targetId);
                if (row == null) throw new BusinessException("项目不存在：" + targetId);
                builder.targetName(str(row[0]))
                        .quotaLimit(dec(row[1]))
                        .quotaUsed(dec(row[2]))
                        .quotaRemaining(dec(row[3]))
                        .quotaType(str(row[4]));
            }
            default -> throw new BusinessException("不支持的目标类型：" + targetType);
        }

        return builder.build();
    }

    private Object[] querySummaryRow(String sql, Long id) {
        try {
            Query q = entityManager.createNativeQuery(sql);
            q.setParameter("id", id);
            Object result = q.getSingleResult();
            if (result instanceof Object[]) return (Object[]) result;
            return new Object[]{result};
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private BigDecimal dec(Object o) {
        return o != null ? new BigDecimal(o.toString()) : BigDecimal.ZERO;
    }

    // ===== 工具方法 =====

    private int buildAndExecute(String sql, BigDecimal amount, Long id) {
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("amount", amount);
        q.setParameter("id", id);
        return q.executeUpdate();
    }

    // ===== 流水 CSV 导出 =====

    private static final int TRANSACTION_EXPORT_LIMIT = 10000;

    public String exportTransactionsCsv(String targetType, Long targetId, Long userId) {
        PageRequest pageable = PageRequest.of(0, TRANSACTION_EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<QuotaTransaction> page;
        if (targetType != null && targetId != null) {
            page = transactionRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable);
        } else if (userId != null) {
            page = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            page = transactionRepository.findAll(pageable);
        }

        String header = "时间,流水ID,用户ID,虚拟KeyID,目标类型,目标ID,额度类型,交易类型,变动金额,变动前余额,变动后余额,备注";
        String rows = page.getContent().stream()
                .map(t -> String.join(",",
                        safe(t.getCreatedAt()), safe(t.getId()), safe(t.getUserId()),
                        safe(t.getVirtualKeyId()), safe(t.getTargetType()), safe(t.getTargetId()),
                        safe(t.getQuotaType()), safe(t.getTransactionType()),
                        safe(t.getAmount()), safe(t.getBalanceBefore()), safe(t.getBalanceAfter()),
                        safeEscape(t.getDescription())))
                .collect(Collectors.joining("\n"));

        return header + "\n" + rows;
    }

    private String safe(Object val) {
        return val == null ? "" : val.toString();
    }

    /** 含逗号的字段用双引号包裹 */
    private String safeEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}

