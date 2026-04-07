package com.aikey.gateway.dispatch;

/**
 * 调度策略类型枚举
 */
public enum DispatchStrategyType {

    ROUND_ROBIN("轮询"),
    WEIGHTED("加权轮询"),
    LOWEST_LATENCY("最低延迟"),
    LOWEST_COST("最低成本");

    private final String description;

    DispatchStrategyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
