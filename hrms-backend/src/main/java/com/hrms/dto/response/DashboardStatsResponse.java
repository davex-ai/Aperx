package com.hrms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalHeadcount;
    private long newHiresThisMonth;
    private long exitsThisYear;
    private double turnoverRatePercent;
    private long pendingLeaveRequests;
    private long openJobPostings;
    private Map<String, Long> departmentDistribution;
    private List<MonthlyHeadcount> headcountTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyHeadcount {
        private String month;
        private long count;
    }
}
