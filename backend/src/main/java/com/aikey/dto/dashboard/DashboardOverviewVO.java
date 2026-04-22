package com.aikey.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {

    /** 今日总调用次数 */
    private long todayCalls;

    /** 今日总 Token 消耗 */
    private long todayTokens;

    /** 今日总费用（元） */
    private BigDecimal todayCost;

    /** 今日平均响应时间（ms） */
    private long avgResponseTime;

    /** 今日调用次数环比昨日变化率（%） */
    private double todayCallsTrend;

    /** 今日 Token 消耗环比昨日变化率（%） */
    private double todayTokensTrend;

    /** 今日费用环比昨日变化率（%） */
    private double todayCostTrend;

    /** 今日平均响应时间环比昨日变化率（%） */
    private double avgResponseTimeTrend;
}
