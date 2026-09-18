package com.kkori.api.admin.dto.response.dashboard;

import java.util.List;
import java.util.Map;

public record AdminDashboardRecordsResponse(
        List<TimeSeriesPoint> photoTrend,
        List<TimeSeriesPoint> logTrend,
        List<TimeSeriesPoint> activeUnitTrend,
        List<EngagementPoint> engagementTrend,
        List<TimeSeriesPoint> cumulativeTrend,
        StreakSummary streak,
        Map<String, Long> featureCombination
) {
}
