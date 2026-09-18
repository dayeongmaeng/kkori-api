package com.kkori.api.admin.dto.response.dashboard;

public record DurationStats(double avgHours, double medianHours, long sampleCount) {
    public static final DurationStats EMPTY = new DurationStats(0, 0, 0);
}
