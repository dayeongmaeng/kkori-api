package com.kkori.api.admin.dto.response.dashboard;

import java.util.List;
import java.util.Map;

public record AdminDashboardMembersResponse(
        long totalMembers,
        long withdrawnMembers,
        List<TimeSeriesPoint> signupTrend,
        List<TimeSeriesPoint> withdrawalTrend,
        Map<String, Long> byProvider,
        Map<String, Long> byStatus
) {
}
