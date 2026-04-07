package com.aikey.gateway.dispatch;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 最低成本调度策略
 *
 * <p>选择inputPrice + outputPrice最低的模型所在渠道。</p>
 */
@Component
public class LowestCostStrategy implements DispatchStrategy {

    @Override
    public Candidate select(List<Candidate> candidates) {
        return candidates.stream()
                .min(Comparator.comparing(c -> {
                    BigDecimal input = c.model().getInputPrice() != null ? c.model().getInputPrice() : BigDecimal.ZERO;
                    BigDecimal output = c.model().getOutputPrice() != null ? c.model().getOutputPrice() : BigDecimal.ZERO;
                    return input.add(output);
                }))
                .orElse(candidates.get(0));
    }
}
