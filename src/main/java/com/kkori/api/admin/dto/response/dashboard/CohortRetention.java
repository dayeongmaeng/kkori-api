package com.kkori.api.admin.dto.response.dashboard;

import java.time.LocalDate;

/** w1/w2/w4/w8은 아직 해당 주차가 도래하지 않았으면 null. */
public record CohortRetention(LocalDate cohortWeekStart, long cohortSize, Double w1, Double w2, Double w4, Double w8) {
}
