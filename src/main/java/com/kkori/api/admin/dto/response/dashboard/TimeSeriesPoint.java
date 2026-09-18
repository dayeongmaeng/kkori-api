package com.kkori.api.admin.dto.response.dashboard;

import java.time.LocalDate;

public record TimeSeriesPoint(LocalDate bucketStart, long value) {
}
