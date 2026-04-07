package com.aikey.gateway.dispatch;

import com.aikey.entity.Channel;
import com.aikey.entity.Model;
import com.aikey.entity.RealKey;
import com.aikey.exception.BusinessException;
import com.aikey.repository.ChannelRepository;
import com.aikey.repository.ModelRepository;
import com.aikey.repository.RealKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 调度服务
 *
 * <p>编排候选渠道筛选和策略选择，返回最终的调度结果。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final ModelRepository modelRepository;
    private final RealKeyRepository realKeyRepository;
    private final RoundRobinStrategy roundRobinStrategy;
    private final WeightedRoundRobinStrategy weightedRoundRobinStrategy;
    private final LowestLatencyStrategy lowestLatencyStrategy;
    private final LowestCostStrategy lowestCostStrategy;

    // 每个渠道的RealKey轮询计数器
    private final ConcurrentHashMap<Long, AtomicInteger> realKeyCounters = new ConcurrentHashMap<>();

    /**
     * 执行调度：筛选候选渠道 → 策略选择 → 选择RealKey
     *
     * @param modelCode       请求的模型编码
     * @param strategyType    调度策略类型
     * @param excludedChannelIds 需要排除的渠道ID（故障转移时使用）
     * @return 调度结果
     */
    public DispatchResult dispatch(String modelCode, DispatchStrategyType strategyType, Set<Long> excludedChannelIds) {
        // 1. 查找所有启用的、匹配该模型编码的Model
        List<Model> models = modelRepository.findByModelCodeAndStatusAndDeleted(modelCode, 1, 0);
        if (models.isEmpty()) {
            throw new BusinessException(404, "Model not found: " + modelCode);
        }

        // 2. 筛选候选：渠道启用、健康、未被排除，且有可用RealKey
        List<DispatchStrategy.Candidate> candidates = new ArrayList<>();
        Map<Long, List<RealKey>> channelRealKeys = new HashMap<>();

        for (Model model : models) {
            Channel channel = model.getChannel();
            if (channel == null || channel.getDeleted() == 1) continue;
            if (channel.getStatus() != 1) continue;
            if (channel.getHealthStatus() != null && channel.getHealthStatus() != 1) continue;
            if (excludedChannelIds != null && excludedChannelIds.contains(channel.getId())) continue;

            // 查找该渠道下可用的RealKey
            if (!channelRealKeys.containsKey(channel.getId())) {
                List<RealKey> realKeys = realKeyRepository.findByChannelIdAndDeletedOrderByCreatedAtDesc(channel.getId(), 0)
                        .stream()
                        .filter(rk -> rk.getStatus() == 1)
                        .filter(rk -> rk.getExpireTime() == null || rk.getExpireTime().isAfter(LocalDateTime.now()))
                        .collect(Collectors.toList());
                channelRealKeys.put(channel.getId(), realKeys);
            }

            List<RealKey> realKeys = channelRealKeys.get(channel.getId());
            if (!realKeys.isEmpty()) {
                candidates.add(new DispatchStrategy.Candidate(channel, model));
            }
        }

        if (candidates.isEmpty()) {
            throw new BusinessException(503, "No available channel for model: " + modelCode);
        }

        // 3. 策略选择
        DispatchStrategy strategy = getStrategy(strategyType);
        DispatchStrategy.Candidate selected = strategy.select(candidates);

        // 4. 选择该渠道的一个RealKey（轮询）
        List<RealKey> realKeys = channelRealKeys.get(selected.channel().getId());
        AtomicInteger counter = realKeyCounters.computeIfAbsent(selected.channel().getId(), k -> new AtomicInteger(0));
        int idx = Math.abs(counter.getAndIncrement()) % realKeys.size();
        RealKey selectedKey = realKeys.get(idx);

        log.info("调度完成: model={}, channel={}, strategy={}, realKeyId={}",
                modelCode, selected.channel().getChannelName(), strategyType, selectedKey.getId());

        return DispatchResult.builder()
                .channel(selected.channel())
                .model(selected.model())
                .realKey(selectedKey)
                .strategyUsed(strategyType)
                .build();
    }

    private DispatchStrategy getStrategy(DispatchStrategyType type) {
        return switch (type) {
            case ROUND_ROBIN -> roundRobinStrategy;
            case WEIGHTED -> weightedRoundRobinStrategy;
            case LOWEST_LATENCY -> lowestLatencyStrategy;
            case LOWEST_COST -> lowestCostStrategy;
        };
    }

    /**
     * 记录调度成功统计
     */
    public void recordSuccess(Channel channel, int responseTimeMs) {
        try {
            long total = (channel.getSuccessCount() != null ? channel.getSuccessCount() : 0) + 1;
            int oldAvg = channel.getAvgResponseTime() != null ? channel.getAvgResponseTime() : 0;
            // 移动平均
            int newAvg = (int) ((oldAvg * (total - 1) + responseTimeMs) / total);

            channel.setSuccessCount(total);
            channel.setAvgResponseTime(newAvg);
        } catch (Exception e) {
            log.warn("更新渠道统计失败: {}", e.getMessage());
        }
    }

    /**
     * 记录调度失败统计
     */
    public void recordFailure(Channel channel) {
        try {
            channel.setFailCount((channel.getFailCount() != null ? channel.getFailCount() : 0) + 1);
        } catch (Exception e) {
            log.warn("更新渠道失败统计异常: {}", e.getMessage());
        }
    }
}
