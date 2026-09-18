package com.kkori.api.admin.dto.response.dashboard;

import java.time.LocalDate;

public record EngagementPoint(LocalDate bucketStart, long activeUnits, long eligibleUnits, double rate) {
}
