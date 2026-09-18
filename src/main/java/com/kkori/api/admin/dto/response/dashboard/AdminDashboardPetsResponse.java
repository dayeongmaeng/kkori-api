package com.kkori.api.admin.dto.response.dashboard;

import java.util.List;
import java.util.Map;

public record AdminDashboardPetsResponse(
        long totalPets,
        List<TimeSeriesPoint> newPetTrend,
        Map<String, Long> bySpecies,
        Map<String, Long> petsPerMemberDistribution
) {
}
