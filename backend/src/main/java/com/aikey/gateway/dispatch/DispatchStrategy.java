package com.aikey.gateway.dispatch;

import com.aikey.entity.Channel;
import com.aikey.entity.Model;

import java.util.List;

/**
 * 调度策略接口
 */
public interface DispatchStrategy {

    /**
     * 从候选列表中选择一个渠道+模型组合
     *
     * @param candidates 候选列表（Channel-Model对）
     * @return 选中的候选
     */
    Candidate select(List<Candidate> candidates);

    /**
     * 候选项（渠道+该渠道下的模型）
     */
    record Candidate(Channel channel, Model model) {}
}
