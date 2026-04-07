package com.aikey.gateway.dispatch;

import com.aikey.entity.Channel;
import com.aikey.entity.Model;
import com.aikey.entity.RealKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调度结果
 *
 * <p>包含选中的渠道、模型和真实Key，供代理转发层使用。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchResult {

    private Channel channel;
    private Model model;
    private RealKey realKey;
    private DispatchStrategyType strategyUsed;
}
