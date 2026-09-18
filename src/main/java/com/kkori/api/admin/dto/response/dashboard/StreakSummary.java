package com.kkori.api.admin.dto.response.dashboard;

import java.util.Map;

/** streak는 date 컬럼 기준, 조회 기간(from~to) 내 데이터만으로 계산한 현재(=to 시점) 연속 기록일수다. */
public record StreakSummary(double avgCurrentStreakDays, int maxCurrentStreakDays, Map<String, Long> distribution) {
}
