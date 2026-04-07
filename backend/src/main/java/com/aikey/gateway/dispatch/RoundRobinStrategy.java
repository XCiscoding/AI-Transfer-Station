package com.aikey.gateway.dispatch;

import com.aikey.entity.Channel;
import com.aikey.entity.Model;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询调度策略
 */
@Component
public class RoundRobinStrategy implements DispatchStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Candidate select(List<Candidate> candidates) {
        int index = Math.abs(counter.getAndIncrement()) % candidates.size();
        return candidates.get(index);
    }
}
