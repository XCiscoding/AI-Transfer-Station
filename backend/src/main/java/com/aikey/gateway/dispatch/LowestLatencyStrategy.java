package com.aikey.gateway.dispatch;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 最低延迟调度策略
 *
 * <p>选择avgResponseTime最低的渠道。</p>
 */
@Component
public class LowestLatencyStrategy implements DispatchStrategy {

    @Override
    public Candidate select(List<Candidate> candidates) {
        return candidates.stream()
                .min(Comparator.comparingInt(c ->
                        c.channel().getAvgResponseTime() != null ? c.channel().getAvgResponseTime() : Integer.MAX_VALUE))
                .orElse(candidates.get(0));
    }
}
