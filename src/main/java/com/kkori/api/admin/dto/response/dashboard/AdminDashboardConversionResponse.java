package com.kkori.api.admin.dto.response.dashboard;

import java.util.List;

public record AdminDashboardConversionResponse(
        DurationStats signupToFirstPet,
        DurationStats petToFirstRecord,
        List<CohortRetention> retention
) {
}
