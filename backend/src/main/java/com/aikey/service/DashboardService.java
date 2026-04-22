package com.aikey.service;

import com.aikey.dto.dashboard.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardOverviewVO getOverview() {
        long todayCalls = queryLong(
                "SELECT COUNT(*) FROM call_logs WHERE DATE(created_at) = CURDATE()");
        long todayTokens = queryLong(
                "SELECT COALESCE(SUM(total_tokens), 0) FROM call_logs WHERE DATE(created_at) = CURDATE()");
        BigDecimal todayCost = queryDecimal(
                "SELECT COALESCE(SUM(cost), 0) FROM call_logs WHERE DATE(created_at) = CURDATE()");
        long avgResponseTime = queryLong(
                "SELECT COALESCE(AVG(response_time), 0) FROM call_logs WHERE DATE(created_at) = CURDATE() AND status = 1");

        long yesterdayCalls = queryLong(
                "SELECT COUNT(*) FROM call_logs WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)");
        long yesterdayTokens = queryLong(
                "SELECT COALESCE(SUM(total_tokens), 0) FROM call_logs WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)");
        BigDecimal yesterdayCost = queryDecimal(
                "SELECT COALESCE(SUM(cost), 0) FROM call_logs WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)");
        long yesterdayAvgResponseTime = queryLong(
                "SELECT COALESCE(AVG(response_time), 0) FROM call_logs WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND status = 1");

        return DashboardOverviewVO.builder()
                .todayCalls(todayCalls)
                .todayTokens(todayTokens)
                .todayCost(todayCost)
                .avgResponseTime(avgResponseTime)
                .todayCallsTrend(calcTrend(todayCalls, yesterdayCalls))
                .todayTokensTrend(calcTrend(todayTokens, yesterdayTokens))
                .todayCostTrend(calcTrend(todayCost.longValue(), yesterdayCost.longValue()))
                .avgResponseTimeTrend(calcTrend(avgResponseTime, yesterdayAvgResponseTime))
                .build();
    }

    private long queryLong(String sql) {
        Long val = jdbcTemplate.queryForObject(sql, Long.class);
        return val == null ? 0L : val;
    }

    private BigDecimal queryDecimal(String sql) {
        BigDecimal val = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return val == null ? BigDecimal.ZERO : val.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算环比变化率，单位 %，保留一位小数
     * 基数为 0 时返回 0 避免除零
     */
    private double calcTrend(long today, long yesterday) {
        if (yesterday == 0) return 0.0;
        double rate = (today - yesterday) * 100.0 / yesterday;
        return Math.round(rate * 10) / 10.0;
    }
}
