package com.kkori.api.admin.dto.response.dashboard;

import java.util.List;

public record AdminDashboardOverviewResponse(
        long totalMembers,
        long dau,
        long wau,
        long mau,
        double stickiness,
        long todayRecordCount,
        List<TimeSeriesPoint> activeUnitTrend
) {
}
