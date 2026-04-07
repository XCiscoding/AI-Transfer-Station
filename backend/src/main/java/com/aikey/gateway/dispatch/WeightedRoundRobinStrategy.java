package com.aikey.gateway.dispatch;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 加权轮询调度策略
 *
 * <p>根据Channel的weight字段进行加权随机选择。
 * weight越大，被选中的概率越高。</p>
 */
@Component
public class WeightedRoundRobinStrategy implements DispatchStrategy {

    @Override
    public Candidate select(List<Candidate> candidates) {
        int totalWeight = candidates.stream()
                .mapToInt(c -> c.channel().getWeight() != null ? c.channel().getWeight() : 100)
                .sum();

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int accumulated = 0;

        for (Candidate candidate : candidates) {
            accumulated += (candidate.channel().getWeight() != null ? candidate.channel().getWeight() : 100);
            if (random < accumulated) {
                return candidate;
            }
        }

        // 理论上不会走到这里
        return candidates.get(candidates.size() - 1);
    }
}
